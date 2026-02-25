package com.matchimban.matchimban_api.settlement.dto.response;

import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;

public record ReceiptConfirmResponse(
        Long settlementId,
        SettlementStatus settlementStatus
) {}