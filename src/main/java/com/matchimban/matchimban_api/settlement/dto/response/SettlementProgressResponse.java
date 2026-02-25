package com.matchimban.matchimban_api.settlement.dto.response;

import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;

public record SettlementProgressResponse(
        Long settlementId,
        SettlementStatus settlementStatus,
        String requestId,
        OcrError error
) {
    public record OcrError(String code, String message) {}
}