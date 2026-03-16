package com.matchimban.matchimban_api.notification.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum NotificationErrorCode implements ErrorCode {

    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "알림 커서 값이 올바르지 않습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    INVALID_FCM_TOKEN(HttpStatus.BAD_REQUEST, "FCM 토큰 값이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(HttpStatus status, String message) {
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
