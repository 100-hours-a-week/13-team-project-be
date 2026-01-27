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
		String key = buildKey(memberId, sid);

		Map<String, String> values = new HashMap<>();
		values.put(FIELD_REFRESH_HASH, hash(refreshToken));
		values.put(FIELD_ISSUED_AT, String.valueOf(Instant.now().toEpochMilli()));
		if (device != null && !device.isBlank()) {
			values.put(FIELD_DEVICE, device);
		}

		redisTemplate.opsForHash().putAll(key, values);
		redisTemplate.expire(key, Duration.ofDays(jwtProperties.refreshTokenExpireDays()));

		return refreshToken;
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
			return Optional.empty();
		}

		// Rotation: 새 refresh 발급 → 해시 갱신 → 기존 refresh는 즉시 무효화
		String newRefreshToken = generateToken();
		Map<String, String> updates = new HashMap<>();
		updates.put(FIELD_REFRESH_HASH, hash(newRefreshToken));
		updates.put(FIELD_ROTATED_AT, String.valueOf(Instant.now().toEpochMilli()));
		if (device != null && !device.isBlank()) {
			updates.put(FIELD_DEVICE, device);
		}

		redisTemplate.opsForHash().putAll(key, updates);
		redisTemplate.expire(key, Duration.ofDays(jwtProperties.refreshTokenExpireDays()));

		return Optional.of(newRefreshToken);
	}

	public void revoke(Long memberId, String sid) {
		redisTemplate.delete(buildKey(memberId, sid));
	}

	private String buildKey(Long memberId, String sid) {
		return KEY_PREFIX + memberId + ":" + sid;
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
}
