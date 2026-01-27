package com.matchimban.matchimban_api.auth.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
	@NotBlank
	@Size(min = 32)
	String secret,
	@NotBlank
	String issuer,
	@Positive
	long accessTokenExpireMinutes,
	@NotBlank
	String cookieName,
	boolean cookieSecure,
	@NotBlank
	String cookieSameSite
) {
}
