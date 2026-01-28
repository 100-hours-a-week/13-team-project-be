package com.matchimban.matchimban_api.auth.kakao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.oauth")
public record KakaoOAuthProperties(
	String authorizeUrl,
	String tokenUrl,
	String userInfoUrl,
	String unlinkUrl,
	String clientId,
	String clientSecret,
	String redirectUri,
	String frontendRedirectUrl,
	String adminKey
) {
}
