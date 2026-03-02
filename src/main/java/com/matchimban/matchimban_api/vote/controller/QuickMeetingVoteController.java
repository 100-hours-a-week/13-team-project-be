package com.matchimban.matchimban_api.vote.controller;

import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.jwt.GuestPrincipal;
import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.vote.dto.request.VoteSubmitRequest;
import com.matchimban.matchimban_api.vote.dto.response.VoteCandidatesResponse;
import com.matchimban.matchimban_api.vote.dto.response.VoteResultsResponse;
import com.matchimban.matchimban_api.vote.dto.response.VoteStatusResponse;
import com.matchimban.matchimban_api.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "QuickMeetingVote", description = "퀵 모임 투표 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quick-meetings")
public class QuickMeetingVoteController {

    private final VoteService voteService;

    @Operation(summary = "퀵 모임 투표 후보 조회")
    @GetMapping("/{meetingId}/votes/{voteId}/candidates")
    public ResponseEntity<VoteCandidatesResponse> getCandidates(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal Object principal
    ) {
        assertGuestMeetingScope(principal, meetingId);
        Long memberId = resolveMemberId(principal);
        return ResponseEntity.ok(voteService.getCandidates(meetingId, voteId, memberId));
    }

    @Operation(summary = "퀵 모임 투표 제출(일괄)")
    @CsrfRequired
    @PostMapping("/{meetingId}/votes/{voteId}/submissions")
    public ResponseEntity<Void> submitVote(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal Object principal,
            @RequestBody @Valid VoteSubmitRequest request
    ) {
        assertGuestMeetingScope(principal, meetingId);
        Long memberId = resolveMemberId(principal);
        voteService.submitVote(meetingId, voteId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "퀵 모임 투표 상태 조회")
    @GetMapping("/{meetingId}/votes/{voteId}/status")
    public ResponseEntity<VoteStatusResponse> getVoteStatus(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal Object principal
    ) {
        assertGuestMeetingScope(principal, meetingId);
        Long memberId = resolveMemberId(principal);
        return ResponseEntity.ok(voteService.getVoteStatus(meetingId, voteId, memberId));
    }

    @Operation(summary = "퀵 모임 투표 결과(Top3) 조회")
    @GetMapping("/{meetingId}/votes/{voteId}/results")
    public ResponseEntity<VoteResultsResponse> getResults(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal Object principal
    ) {
        assertGuestMeetingScope(principal, meetingId);
        Long memberId = resolveMemberId(principal);
        return ResponseEntity.ok(voteService.getResults(meetingId, voteId, memberId));
    }

    private void assertGuestMeetingScope(Object principal, Long pathMeetingId) {
        if (principal instanceof GuestPrincipal gp) {
            if (gp.meetingId() == null || !gp.meetingId().equals(pathMeetingId)) {
                throw new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT);
            }
        }
    }

    private Long resolveMemberId(Object principal) {
        if (principal instanceof MemberPrincipal mp) return mp.memberId();
        if (principal instanceof GuestPrincipal gp) return gp.memberId();
        throw new ApiException(AuthErrorCode.UNAUTHORIZED);
    }
}