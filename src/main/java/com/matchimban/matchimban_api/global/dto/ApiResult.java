package com.matchimban.matchimban_api.global.dto;

public record ApiResult<T>(
	String message,
	T data
) {
	public static <T> ApiResult<T> of(String message, T data) {
		return new ApiResult<>(message, data);
	}

	public static ApiResult<Void> of(String message) {
		return new ApiResult<>(message, null);
	}
}
