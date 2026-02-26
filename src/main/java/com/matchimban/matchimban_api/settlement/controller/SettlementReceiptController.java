package com.matchimban.matchimban_api.settlement.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.settlement.dto.request.ReceiptConfirmRequest;
import com.matchimban.matchimban_api.settlement.dto.request.ReceiptUploadUrlRequest;
import com.matchimban.matchimban_api.settlement.dto.response.ReceiptConfirmResponse;
import com.matchimban.matchimban_api.settlement.dto.response.ReceiptUploadUrlResponse;
import com.matchimban.matchimban_api.settlement.service.SettlementReceiptConfirmService;
import com.matchimban.matchimban_api.settlement.service.SettlementReceiptUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings/{meetingId}/settlement/receipt")
public class SettlementReceiptController {

    private final SettlementReceiptUploadService receiptUploadService;
    private final SettlementReceiptConfirmService receiptConfirmService;

    @PostMapping("/upload-url")
    public ReceiptUploadUrlResponse createUploadUrl(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ReceiptUploadUrlRequest request
    ) {
        return receiptUploadService.createUploadUrl(meetingId, principal.memberId(), request);
    }

    @PostMapping("/confirm")
    public ReceiptConfirmResponse confirm(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ReceiptConfirmRequest request
    ) {
        return receiptConfirmService.confirm(meetingId, principal.memberId(), request);
    }
}