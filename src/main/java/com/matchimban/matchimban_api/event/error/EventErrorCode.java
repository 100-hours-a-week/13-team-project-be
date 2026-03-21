package com.matchimban.matchimban_api.event.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EventErrorCode implements ErrorCode {

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),
    EVENT_ISSUE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 쿠폰 발급 요청을 찾을 수 없습니다."),
    EVENT_ISSUE_CURSOR_INVALID(HttpStatus.BAD_REQUEST, "이벤트 발급 요청 값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    EventErrorCode(HttpStatus status, String message) {
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
