package com.matchimban.matchimban_api.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	String secret,
	String issuer,
	long accessTokenExpireMinutes,
	String cookieName,
	boolean cookieSecure,
	String cookieSameSite
) {
}
