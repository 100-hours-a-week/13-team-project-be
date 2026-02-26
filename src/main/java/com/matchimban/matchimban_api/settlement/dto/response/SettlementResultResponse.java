package com.matchimban.matchimban_api.settlement.dto.response;

import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;

import java.math.BigDecimal;
import java.util.List;

public record SettlementResultResponse(
        Long settlementId,
        SettlementStatus settlementStatus,
        List<Row> participants
) {
    public record Row(
            Long meetingParticipantId,
            Long memberId,
            String nickname,
            String profileImageUrl,
            BigDecimal amountDue,
            PaymentStatus paymentStatus
    ) {}
}