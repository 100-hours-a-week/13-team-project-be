package com.matchimban.matchimban_api.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse")
public record ErrorResponse(
	@Schema(example = "invalid_request") String message,
	@Schema(example = "User denied access") String detail
) {
	public static ErrorResponse of(String message) {
		return new ErrorResponse(message, null);
	}

	public static ErrorResponse of(String message, String detail) {
		return new ErrorResponse(message, detail);
	}
}
