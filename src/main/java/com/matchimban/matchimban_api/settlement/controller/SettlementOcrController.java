package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.response.OcrTriggerResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementOcrTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement")
public class SettlementOcrController {

    private final SettlementOcrTriggerService ocrTriggerService;

    @PostMapping("/ocr")
    public OcrTriggerResponse trigger(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ocrTriggerService.trigger(meetingId, principal.memberId());
    }
}