package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementStateResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementStateController {

    private final SettlementStateService settlementStateService;

    @GetMapping("/state")
    public SettlementStateResponse getState(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return settlementStateService.getState(meetingId, principal.memberId());
    }
}