package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.request.OpenSelectionRequest;
import com.matchimban.matchimban_api.settlement.dto.response.OpenSelectionResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementOpenSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementOpenSelectionController {

    private final SettlementOpenSelectionService service;

    @PostMapping("/open-selection")
    public OpenSelectionResponse openSelection(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody OpenSelectionRequest request
    ) {
        return service.open(meetingId, principal.memberId(), request);
    }
}