package com.matchimban.matchimban_api.restaurant.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ReviewErrorCode implements ErrorCode {

    // 403
    FORBIDDEN_NOT_ACTIVE_PARTICIPANT(HttpStatus.FORBIDDEN, "활성 참여자가 아닙니다."),

    // 404
    FINAL_SELECTION_NOT_FOUND(HttpStatus.NOT_FOUND, "최종 선택 결과가 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),

    // 409
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 작성한 리뷰가 있습니다.");

    private final HttpStatus status;
    private final String message;

    ReviewErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage() { return message; }
    @Override public String getCode() { return name(); }
}