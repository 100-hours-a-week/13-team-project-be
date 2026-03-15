package com.matchimban.matchimban_api.ragchat.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum RagChatErrorCode implements ErrorCode {

	FORBIDDEN_USER_ID(HttpStatus.FORBIDDEN, "forbidden_user_id"),
	RAG_ENGINE_BAD_REQUEST(HttpStatus.BAD_REQUEST, "rag_engine_bad_request"),
	RAG_ENGINE_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "rag_engine_unavailable"),
	RAG_ENGINE_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "rag_engine_response_invalid"),
	RAG_ENGINE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "rag_engine_timeout"),
	RAG_ENGINE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "rag_engine_failed");

	private final HttpStatus status;
	private final String message;

	RagChatErrorCode(HttpStatus status, String message) {
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
