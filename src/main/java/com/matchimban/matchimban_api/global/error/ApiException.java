package com.matchimban.matchimban_api.global.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
	private final HttpStatus status;
	private final String detail;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
		this.detail = null;
	}

	public ApiException(HttpStatus status, String message, String detail) {
		super(message);
		this.status = status;
		this.detail = detail;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getDetail() {
		return detail;
	}
}
