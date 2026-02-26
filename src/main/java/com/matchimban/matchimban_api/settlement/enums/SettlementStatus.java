package com.matchimban.matchimban_api.settlement.enums;

public enum SettlementStatus {
    NOT_STARTED,
    RECEIPT_UPLOADED,
    OCR_PROCESSING,
    OCR_FAILED,
    OCR_SUCCEEDED,
    SELECTION_OPEN,
    CALCULATING,
    RESULT_READY,
    COMPLETED
}