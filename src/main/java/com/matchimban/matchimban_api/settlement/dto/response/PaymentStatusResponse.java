package com.matchimban.matchimban_api.settlement.dto.response;

import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;

public record PaymentStatusResponse(
        Long settlementId,
        Long meetingParticipantId,
        PaymentStatus paymentStatus
) {}