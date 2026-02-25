package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementItemsResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementItemsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementItemsController {

    private final SettlementItemsService settlementItemsService;

    @GetMapping("/items")
    public SettlementItemsResponse getItems(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return settlementItemsService.getItems(meetingId, principal.memberId());
    }
}