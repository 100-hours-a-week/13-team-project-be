package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementResultResponse;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementWaitingResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementReadController {

    private final SettlementReadService settlementReadService;

    @GetMapping("/waiting")
    public SettlementWaitingResponse waiting(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return settlementReadService.getWaiting(meetingId, principal.memberId());
    }

    @GetMapping("/result")
    public SettlementResultResponse result(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return settlementReadService.getResult(meetingId, principal.memberId());
    }
}