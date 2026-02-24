package com.matchimban.matchimban_api.auth.oauth.service;

import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * OAuth state는 CSRF 방지용 "1회성 토큰"이다.
 * - issueState: Redis에 저장(짧은 TTL)
 * - consumeState: 검증 + 즉시 삭제(재사용 방지)
 */
@Service
public class OAuthStateService {

	private static final Duration STATE_TTL = Duration.ofMinutes(5);
	private static final String OAUTH_STATE_KEY_PREFIX = "oauth_state:";

	private final StringRedisTemplate redisTemplate;

	public OAuthStateService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public String issueState(OAuthProviderType providerType) {
		String state = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set(buildKey(providerType, state), "1", STATE_TTL);
		return state;
	}

	public boolean consumeState(OAuthProviderType providerType, String state) {
		if (state == null || state.isBlank()) {
			return false;
		}
		return Boolean.TRUE.equals(redisTemplate.delete(buildKey(providerType, state)));
	}

	private String buildKey(OAuthProviderType providerType, String state) {
		return OAUTH_STATE_KEY_PREFIX + providerType.provider() + ":" + state;
	}
}

