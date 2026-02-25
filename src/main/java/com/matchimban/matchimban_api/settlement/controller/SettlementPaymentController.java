package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.PaymentStatusResponse;
import com.matchimban.matchimban_api.settlement.dto.response.RemindUnpaidResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementPaymentController {

    private final SettlementPaymentService paymentService;

    @PostMapping("/payments/me/request")
    public PaymentStatusResponse requestMyPayment(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return paymentService.requestMyPayment(meetingId, principal.memberId());
    }

    @PostMapping("/payments/{participantId}/confirm")
    public PaymentStatusResponse confirmPayment(
            @PathVariable Long meetingId,
            @PathVariable Long participantId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return paymentService.confirmPayment(meetingId, principal.memberId(), participantId);
    }

    @PostMapping("/remind-unpaid")
    public RemindUnpaidResponse remindUnpaid(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return paymentService.remindUnpaid(meetingId, principal.memberId());
    }
}