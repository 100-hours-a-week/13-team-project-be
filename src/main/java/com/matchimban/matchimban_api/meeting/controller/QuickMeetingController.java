package com.matchimban.matchimban_api.meeting.controller;

import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.meeting.dto.request.QuickMeetingEnterRequest;
import com.matchimban.matchimban_api.meeting.dto.response.QuickMeetingDetailResponse;
import com.matchimban.matchimban_api.meeting.dto.response.QuickMeetingEnterResponse;
import com.matchimban.matchimban_api.meeting.service.QuickMeetingReadService;
import com.matchimban.matchimban_api.meeting.service.QuickMeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quick-meetings")
public class QuickMeetingController {

    private final QuickMeetingService quickMeetingService;
    private final QuickMeetingReadService quickMeetingReadService;

    @CsrfRequired
    @PostMapping("/enter")
    public ResponseEntity<QuickMeetingEnterResponse> enter(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody QuickMeetingEnterRequest request
    ) {
        QuickMeetingService.EnterResult result = quickMeetingService.enter(principal, request);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.guestAccessCookie() != null) {
            builder.header(HttpHeaders.SET_COOKIE, result.guestAccessCookie().toString());
        }
        return builder.body(result.body());
    }

    @GetMapping("/{inviteCode:[A-Za-z0-9]{8}}")
    public ResponseEntity<QuickMeetingDetailResponse> getQuickMeetingDetail(
            @PathVariable String inviteCode
    ) {
        return ResponseEntity.ok(quickMeetingReadService.getQuickMeetingDetailByInviteCode(inviteCode));
    }
}