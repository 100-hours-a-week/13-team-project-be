package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.request.MenuSelectionConfirmRequest;
import com.matchimban.matchimban_api.settlement.dto.response.MenuSelectionConfirmResponse;
import com.matchimban.matchimban_api.settlement.dto.response.MenuSelectionResponse;
import com.matchimban.matchimban_api.settlement.service.MenuSelectionConfirmService;
import com.matchimban.matchimban_api.settlement.service.MenuSelectionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementSelectionController {

    private final MenuSelectionQueryService queryService;
    private final MenuSelectionConfirmService confirmService;

    @GetMapping("/selection")
    public MenuSelectionResponse getSelection(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return queryService.getSelection(meetingId, principal.memberId());
    }

    @PostMapping("/selection/confirm")
    public MenuSelectionConfirmResponse confirmSelection(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody MenuSelectionConfirmRequest request
    ) {
        return confirmService.confirm(meetingId, principal.memberId(), request);
    }
}