package com.matchimban.matchimban_api.settlement.error;

import com.matchimban.matchimban_api.global.error.code.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SettlementErrorCode implements ErrorCode {

    QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED(HttpStatus.CONFLICT, "퀵 모임에서는 정산을 사용할 수 없습니다."),
    UNSUPPORTED_RECEIPT_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "영수증 이미지는 jpeg/png만 업로드할 수 있습니다."),

    INVALID_RECEIPT_OBJECT_KEY(HttpStatus.BAD_REQUEST, "영수증 objectKey 형식이 올바르지 않습니다."),
    RECEIPT_CONFIRM_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 단계에서는 영수증을 확정할 수 없습니다."),

    OCR_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 단계에서는 OCR을 실행할 수 없습니다."),
    RECEIPT_NOT_UPLOADED(HttpStatus.CONFLICT, "영수증 업로드가 필요합니다."),
    OCR_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "OCR이 이미 진행 중입니다."),

    SETTLEMENT_NOT_FOUND(org.springframework.http.HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없습니다."),
    OCR_RESULT_NOT_READY(org.springframework.http.HttpStatus.CONFLICT, "OCR 결과가 아직 준비되지 않았습니다."),

    SELECTION_NOT_OPEN(org.springframework.http.HttpStatus.CONFLICT, "현재 단계에서는 메뉴 배정을 할 수 없습니다."),
    OPEN_SELECTION_NOT_ALLOWED(org.springframework.http.HttpStatus.CONFLICT, "현재 단계에서는 메뉴 배정을 시작할 수 없습니다."),
    INVALID_RECEIPT_ITEM_ID(org.springframework.http.HttpStatus.BAD_REQUEST, "수정 대상 영수증 항목이 올바르지 않습니다."),

    RESULT_NOT_READY(HttpStatus.CONFLICT, "정산 결과가 아직 준비되지 않았습니다."),
    PAYMENT_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 단계에서는 송금 상태를 변경할 수 없습니다."),
    COMPLETE_NOT_ALLOWED(HttpStatus.CONFLICT, "현재 단계에서는 정산 완료를 처리할 수 없습니다."),

    INVALID_ITEM_ID(HttpStatus.BAD_REQUEST, "선택한 메뉴 항목이 올바르지 않습니다."),
    EMPTY_SELECTION(HttpStatus.BAD_REQUEST, "선택한 메뉴가 없습니다."),
    ONLY_MEMBER_ALLOWED(HttpStatus.FORBIDDEN, "모임원만 수행할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    SettlementErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getMessage() { return message; }
    @Override public String getCode() { return name(); }
}