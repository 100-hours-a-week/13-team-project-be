package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementProgressResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementProgressService;
import com.matchimban.matchimban_api.settlement.service.SettlementProgressSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementProgressController {

    private final SettlementProgressService settlementProgressService;
    private final SettlementProgressSseService settlementProgressSseService;

    @GetMapping("/progress")
    public SettlementProgressResponse getProgress(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return settlementProgressService.getProgress(meetingId, principal.memberId());
    }

    @GetMapping(path = "/progress/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return settlementProgressSseService.subscribe(meetingId, principal.memberId());
    }
}
