package com.matchimban.matchimban_api.settlement.enums;

public enum SettlementNextAction {
    GO_RECEIPT_UPLOAD,
    GO_OCR_LOADING,
    GO_OCR_FAILED,
    GO_OCR_EDIT,
    GO_MENU_SELECTION,
    GO_WAITING,
    GO_RESULT,
    GO_COMPLETED,

    // 모임원: 아직 오픈 전
    GO_MEETING_DETAIL_WITH_MODAL
}