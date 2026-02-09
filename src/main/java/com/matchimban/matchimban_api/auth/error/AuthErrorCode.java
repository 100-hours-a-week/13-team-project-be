package com.matchimban.matchimban_api.auth.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AuthErrorCode implements ErrorCode {

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
    INVALID_OAUTH_STATE(HttpStatus.UNAUTHORIZED, "OAuth state가 유효하지 않습니다."),

    // 400
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

    // 403
    OAUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "OAuth 인증이 거부되었습니다."),
    ACCOUNT_DELETED(HttpStatus.FORBIDDEN, "삭제된 계정입니다."),
    AGREEMENT_REQUIRED(HttpStatus.FORBIDDEN, "약관 동의가 필요합니다."),
    PREFERENCES_REQUIRED(HttpStatus.FORBIDDEN, "취향 설정이 필요합니다."),
    DEV_ACCOUNT_REQUIRED(HttpStatus.FORBIDDEN, "개발용 계정만 가능합니다."),

    // 502/503 (외부 인증 제공자 장애)
    KAKAO_CIRCUIT_OPEN(HttpStatus.SERVICE_UNAVAILABLE, "카카오 인증이 일시적으로 불가능합니다."),
    KAKAO_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 토큰 요청에 실패했습니다."),
    KAKAO_USERINFO_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "카카오 사용자 정보 요청에 실패했습니다."),
    KAKAO_UNLINK_FAILED(HttpStatus.BAD_GATEWAY, "카카오 연결 해제에 실패했습니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage() { return message; }
    @Override public String getCode() { return name(); }
}
