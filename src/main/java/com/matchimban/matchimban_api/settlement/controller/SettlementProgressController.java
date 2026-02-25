package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementProgressResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementProgressController {

    private final SettlementProgressService settlementProgressService;

    @GetMapping("/progress")
    public SettlementProgressResponse getProgress(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return settlementProgressService.getProgress(meetingId, principal.memberId());
    }
}