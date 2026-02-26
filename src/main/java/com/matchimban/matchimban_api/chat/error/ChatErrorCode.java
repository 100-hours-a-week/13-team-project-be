package com.matchimban.matchimban_api.chat.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ChatErrorCode implements ErrorCode {

	FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden"),
	INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "invalid_message_type"),
	INVALID_MESSAGE_CONTENT(HttpStatus.BAD_REQUEST, "invalid_message_content"),
	INVALID_READ_POINTER(HttpStatus.BAD_REQUEST, "invalid_read_pointer");

	private final HttpStatus status;
	private final String message;

	ChatErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public HttpStatus getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getCode() {
		return name();
	}
}
