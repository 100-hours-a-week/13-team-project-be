package com.matchimban.matchimban_api.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

	private static final String KEY_PREFIX = "refresh:";
	private static final String INDEX_PREFIX = "refresh_idx:"; // refreshHash -> memberId:sid 역인덱스 키 프리픽스
	private static final String FIELD_REFRESH_HASH = "refreshHash";
	private static final String FIELD_ISSUED_AT = "issuedAt";
	private static final String FIELD_ROTATED_AT = "rotatedAt";
	private static final String FIELD_DEVICE = "device";
	// refresh 로테이션(compare-and-set + idx 교체)을 Redis 내부에서 원자적으로 수행하는 Lua 스크립트
	// (동시 요청 레이스로 인한 중복 발급/인덱스 꼬임 방지)
	private static final DefaultRedisScript<Long> ROTATE_SCRIPT = buildRotateScript();

	private final StringRedisTemplate redisTemplate;
	private final JwtProperties jwtProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(
		StringRedisTemplate redisTemplate,
		JwtProperties jwtProperties
	) {
		this.redisTemplate = redisTemplate;
		this.jwtProperties = jwtProperties;
	}

	public String issue(Long memberId, String sid, String device) {
		// 발급된 refresh 원문은 저장하지 않고 해시만 저장한다.
		String refreshToken = generateToken();
		String refreshHash = hash(refreshToken);
		String key = buildKey(memberId, sid);

		Map<String, String> values = new HashMap<>();
		values.put(FIELD_REFRESH_HASH, refreshHash);
		values.put(FIELD_ISSUED_AT, String.valueOf(Instant.now().toEpochMilli()));
		if (device != null && !device.isBlank()) {
			values.put(FIELD_DEVICE, device);
		}

		redisTemplate.opsForHash().putAll(key, values);
		Duration ttl = Duration.ofDays(jwtProperties.refreshTokenExpireDays());
		redisTemplate.expire(key, ttl);
		// refreshHash로 세션을 찾을 수 있도록 역인덱스를 함께 저장한다.
		redisTemplate.opsForValue().set(buildIndexKey(refreshHash), buildIndexValue(memberId, sid), ttl);

		return refreshToken;
	}

	public Optional<RefreshSession> resolveSession(String refreshToken) {
		// refresh 토큰만으로 memberId/sid를 역조회한다.
		String refreshHash = hash(refreshToken);
		String indexValue = redisTemplate.opsForValue().get(buildIndexKey(refreshHash));
		if (indexValue == null || indexValue.isBlank()) {
			return Optional.empty();
		}

		String[] parts = indexValue.split(":", 2); // "memberId:sid" 형식
		if (parts.length != 2) {
			return Optional.empty();
		}
		try {
			Long memberId = Long.valueOf(parts[0]);
			String sid = parts[1];
			if (sid.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(new RefreshSession(memberId, sid));
		} catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	public Optional<String> rotate(Long memberId, String sid, String presentedToken, String device) {
		String key = buildKey(memberId, sid); // refresh 세션 "본체" 키: refresh:{memberId}:{sid}
		String presentedHash = hash(presentedToken); // 요청으로 들어온 refresh 토큰(원문)을 저장용 해시로 변환한다.

		// Rotation: 새 refresh 발급 → 해시 갱신 → 기존 refresh는 즉시 무효화
		// 새 refresh 토큰 "원문"은 클라이언트 쿠키로 내려줘야 하므로 JVM에서 생성한다.
		// (Lua에는 원문을 넘기지 않고 해시만 넘긴다: Redis 원문 저장 금지 정책 유지)
		String newRefreshToken = generateToken(); // 새 refresh 토큰(원문) - 성공 시에만 응답 쿠키로 내려간다.
		String newRefreshHash = hash(newRefreshToken); // Redis에는 원문 대신 해시만 저장한다.

		Duration ttl = Duration.ofDays(jwtProperties.refreshTokenExpireDays()); // refresh 세션 TTL(초로 변환해 Lua에 전달)
		String deviceValue = (device == null) ? "" : device; // Lua는 null 전달이 번거로워 빈 문자열로 보정한다.

		// Lua 1회 실행으로 "검증 + 교체 + 인덱스 갱신"을 원자적으로 처리한다.
		// KEYS:
		//   1) refresh:{memberId}:{sid}            - 세션 본체
		//   2) refresh_idx:{presentedHash}        - 기존 refreshHash 역인덱스
		//   3) refresh_idx:{newRefreshHash}       - 신규 refreshHash 역인덱스
		// ARGV:
		//   1) presentedHash, 2) newRefreshHash, 3) ttlSeconds, 4) rotatedAtMillis,
		//   5) device(옵션), 6) indexValue(memberId:sid), 7) indexPrefix("refresh_idx:")
		Long result = redisTemplate.execute(
			ROTATE_SCRIPT,
			List.of(
				key, // KEYS[1]
				buildIndexKey(presentedHash), // KEYS[2]
				buildIndexKey(newRefreshHash) // KEYS[3]
			),
			presentedHash, // ARGV[1]
			newRefreshHash, // ARGV[2]
			String.valueOf(ttl.getSeconds()), // ARGV[3]
			String.valueOf(Instant.now().toEpochMilli()), // ARGV[4]
			deviceValue, // ARGV[5]
			buildIndexValue(memberId, sid), // ARGV[6]
			INDEX_PREFIX // ARGV[7]
		);

		// result:
		//  1  = 성공(세션 해시/인덱스 교체 완료)
		//  0  = 세션 없음(만료/삭제)
		// -1  = 해시 불일치(재사용/탈취/레이스) → Lua 내부에서 세션/인덱스 정리
		if (result == null || result <= 0) {
			// 실패한 경우, JVM에서 미리 생성한 newRefreshToken은 응답으로 내려가지 않으므로 유효해지지 않는다.
			return Optional.empty();
		}

		// 성공한 경우에만 새 refresh 토큰 원문을 반환해 쿠키로 내려준다.
		return Optional.of(newRefreshToken);
	}

	public void revoke(Long memberId, String sid) {
		String key = buildKey(memberId, sid);
		Object storedHash = redisTemplate.opsForHash().get(key, FIELD_REFRESH_HASH);
		redisTemplate.delete(key);
		// 세션 삭제 시 역인덱스도 함께 제거한다.
		if (storedHash != null) {
			deleteIndex(storedHash.toString());
		}
	}

	public void revokeAll(Long memberId) {
		// memberId 기준으로 모든 세션 키를 찾아 일괄 폐기한다.
		String pattern = KEY_PREFIX + memberId + ":*";
		var keys = redisTemplate.keys(pattern);
		if (keys == null || keys.isEmpty()) {
			return;
		}
		var indexKeys = new java.util.ArrayList<String>(); // 세션 키에 매핑된 역인덱스를 함께 수집한다.
		for (String key : keys) {
			Object storedHash = redisTemplate.opsForHash().get(key, FIELD_REFRESH_HASH);
			if (storedHash != null) {
				indexKeys.add(buildIndexKey(storedHash.toString()));
			}
		}
		redisTemplate.delete(keys);
		if (!indexKeys.isEmpty()) {
			redisTemplate.delete(indexKeys);
		}
	}

	private String buildKey(Long memberId, String sid) {
		return KEY_PREFIX + memberId + ":" + sid;
	}

	private String buildIndexKey(String refreshHash) { // refreshHash -> sessionKey 역인덱스 키
		return INDEX_PREFIX + refreshHash;
	}

	private String buildIndexValue(Long memberId, String sid) { // 역인덱스 값: "memberId:sid"
		return memberId + ":" + sid;
	}

	private void deleteIndex(String refreshHash) { // 역인덱스 삭제 헬퍼
		if (refreshHash == null || refreshHash.isBlank()) {
			return;
		}
		redisTemplate.delete(buildIndexKey(refreshHash));
	}

	private String generateToken() {
		// 256-bit 랜덤 토큰을 URL-safe 문자열로 발급한다.
		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private String hash(String value) {
		// Redis 저장용 해시
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	public record RefreshSession(Long memberId, String sid) { // refresh 토큰으로 얻은 세션 정보
	}

	private static DefaultRedisScript<Long> buildRotateScript() {
		String script = """
			-- KEYS[1] = refresh:{memberId}:{sid}
			-- KEYS[2] = refresh_idx:{presentedHash}
			-- KEYS[3] = refresh_idx:{newHash}
			-- ARGV[1] = presentedHash
			-- ARGV[2] = newHash
			-- ARGV[3] = ttlSeconds
			-- ARGV[4] = rotatedAtMillis
			-- ARGV[5] = device (optional, empty allowed)
			-- ARGV[6] = indexValue (memberId:sid)
			-- ARGV[7] = indexPrefix (refresh_idx:)

			-- 반환값:
			--  1  : 성공(교체 완료)
			--  0  : 세션 없음
			-- -1  : 해시 불일치(재사용/탈취/레이스) → 세션/인덱스 폐기
			local stored = redis.call('HGET', KEYS[1], 'refreshHash')
			if not stored then
			  return 0
			end
			if stored ~= ARGV[1] then
			  redis.call('DEL', KEYS[1])
			  redis.call('DEL', KEYS[2])
			  redis.call('DEL', ARGV[7] .. stored)
			  return -1
			end
			redis.call('HSET', KEYS[1], 'refreshHash', ARGV[2], 'rotatedAt', ARGV[4])
			if ARGV[5] ~= nil and ARGV[5] ~= '' then
			  redis.call('HSET', KEYS[1], 'device', ARGV[5])
			end
			redis.call('EXPIRE', KEYS[1], ARGV[3])
			redis.call('DEL', KEYS[2])
			redis.call('SET', KEYS[3], ARGV[6], 'EX', ARGV[3])
			return 1
			""";
		DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptText(script);
		redisScript.setResultType(Long.class);
		return redisScript;
	}
}
