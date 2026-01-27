package com.matchimban.matchimban_api.auth.kakao.dto;

public record KakaoAuthCallbackResponse(
	String redirectUrl,
	String memberStatus
) {
}
