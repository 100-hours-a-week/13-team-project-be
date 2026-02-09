package com.matchimban.matchimban_api.member.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MemberErrorCode implements ErrorCode {

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "unauthorized"),

    // 400
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid_request"),
    AGREEMENT_NOT_FOUND_AS_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid_request"),

    // 409
    ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "already_withdrawn");


    private final HttpStatus status;
    private final String message;

    MemberErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage() { return message; }
    @Override public String getCode() { return name(); }
}
