package com.matchimban.matchimban_api.auth.oauth.service;

import org.springframework.http.ResponseCookie;

public record OAuthCallbackResult(
	Long memberId,
	String memberStatus,
	String redirectUrl,
	ResponseCookie accessTokenCookie,
	ResponseCookie refreshTokenCookie
) {
}

