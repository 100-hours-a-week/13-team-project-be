package com.matchimban.matchimban_api.settlement.dto.response;

import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;

public record SettlementWaitingResponse(
        Long settlementId,
        SettlementStatus settlementStatus,
        long confirmedCount,
        long totalCount
) {}