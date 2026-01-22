package com.matchimban.matchimban_api.auth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse(
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("token_type") String tokenType,
	@JsonProperty("refresh_token") String refreshToken,
	@JsonProperty("expires_in") Long expiresIn,
	@JsonProperty("refresh_token_expires_in") Long refreshTokenExpiresIn,
	@JsonProperty("id_token") String idToken,
	@JsonProperty("scope") String scope
) {}
