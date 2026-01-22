package com.matchimban.matchimban_api.auth.kakao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record KakaoAuthCodeRequest(
	@Schema(description = "OAuth error code")
	String error,
	@Schema(name = "error_description", description = "OAuth error description")
	String error_description,
	@Schema(description = "CSRF protection state")
	String state,
	@Schema(description = "Authorization code")
	String code
) {
	public String errorDescription() {
		return error_description;
	}
}
