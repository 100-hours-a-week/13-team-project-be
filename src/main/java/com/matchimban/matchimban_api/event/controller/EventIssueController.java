package com.matchimban.matchimban_api.event.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.event.dto.response.EventIssueRequestResponse;
import com.matchimban.matchimban_api.event.dto.response.EventIssueStatusResponse;
import com.matchimban.matchimban_api.event.service.EventIssueRequestService;
import com.matchimban.matchimban_api.event.service.EventIssueStatusService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Event", description = "이벤트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/{eventId}/issue-requests")
public class EventIssueController {

    private final EventIssueRequestService eventIssueRequestService;
    private final EventIssueStatusService eventIssueStatusService;

    @Operation(summary = "이벤트 쿠폰 발급 요청", description = "선착순 쿠폰 발급 요청을 대기열에 접수한다.")
    @PostMapping
    public ResponseEntity<ApiResult<EventIssueRequestResponse>> submit(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long eventId
    ) {
        EventIssueRequestResponse response = eventIssueRequestService.submit(principal.memberId(), eventId);
        return ResponseEntity.ok(ApiResult.of("event_issue_request_processed", response));
    }

    @Operation(summary = "내 이벤트 쿠폰 발급 상태 조회", description = "새로고침 후에도 현재 발급 상태를 polling으로 조회한다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResult<EventIssueStatusResponse>> getCurrentStatus(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long eventId
    ) {
        EventIssueStatusResponse response = eventIssueStatusService.getCurrentStatus(principal.memberId(), eventId);
        return ResponseEntity.ok(ApiResult.of("event_issue_status_loaded", response));
    }
}
