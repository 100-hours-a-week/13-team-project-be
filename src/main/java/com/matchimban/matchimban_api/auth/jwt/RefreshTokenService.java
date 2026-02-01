package com.matchimban.matchimban_api.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

	private static final String KEY_PREFIX = "refresh:";
	private static final String INDEX_PREFIX = "refresh_idx:"; // refreshHash -> memberId:sid 역인덱스 키 프리픽스
	private static final String FIELD_REFRESH_HASH = "refreshHash";
	private static final String FIELD_ISSUED_AT = "issuedAt";
	private static final String FIELD_ROTATED_AT = "rotatedAt";
	private static final String FIELD_DEVICE = "device";

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
		String key = buildKey(memberId, sid);
		Object storedHash = redisTemplate.opsForHash().get(key, FIELD_REFRESH_HASH);
		if (storedHash == null) {
			return Optional.empty();
		}

		String presentedHash = hash(presentedToken);
		if (!presentedHash.equals(storedHash.toString())) {
			//  즉시 세션 폐기 (sid 강제 로그아웃)
			redisTemplate.delete(key);
			// 잘못된 refresh 재사용 시 역인덱스도 함께 정리한다.
			deleteIndex(storedHash.toString());
			deleteIndex(presentedHash);
			return Optional.empty();
		}

		// Rotation: 새 refresh 발급 → 해시 갱신 → 기존 refresh는 즉시 무효화
		String newRefreshToken = generateToken();
		String newRefreshHash = hash(newRefreshToken);
		Map<String, String> updates = new HashMap<>();
		updates.put(FIELD_REFRESH_HASH, newRefreshHash);
		updates.put(FIELD_ROTATED_AT, String.valueOf(Instant.now().toEpochMilli()));
		if (device != null && !device.isBlank()) {
			updates.put(FIELD_DEVICE, device);
		}

		redisTemplate.opsForHash().putAll(key, updates);
		Duration ttl = Duration.ofDays(jwtProperties.refreshTokenExpireDays());
		redisTemplate.expire(key, ttl);
		// 기존 refresh 해시 인덱스를 제거하고 신규 해시로 교체한다.
		deleteIndex(presentedHash);
		redisTemplate.opsForValue().set(buildIndexKey(newRefreshHash), buildIndexValue(memberId, sid), ttl);

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
		// Redis 저장용 해시 (원문 저장 금지 정책)
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	public record RefreshSession(Long memberId, String sid) { // refresh 토큰으로 해석된 세션 정보
	}
}
