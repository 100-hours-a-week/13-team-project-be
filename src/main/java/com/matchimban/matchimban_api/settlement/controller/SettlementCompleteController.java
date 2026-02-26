package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementCompleteResponse;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementCompletedResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementCompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementCompleteController {

    private final SettlementCompleteService completeService;

    @PostMapping("/complete")
    public SettlementCompleteResponse complete(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return completeService.complete(meetingId, principal.memberId());
    }

    @GetMapping("/completed")
    public SettlementCompletedResponse completed(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return completeService.getCompleted(meetingId, principal.memberId());
    }
}