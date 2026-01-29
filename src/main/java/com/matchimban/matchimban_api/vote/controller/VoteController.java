package com.matchimban.matchimban_api.vote.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.vote.dto.response.FinalSelectionResponse;
import com.matchimban.matchimban_api.vote.dto.request.FinalSelectionRequest;
import com.matchimban.matchimban_api.vote.dto.request.VoteSubmitRequest;
import com.matchimban.matchimban_api.vote.dto.response.*;
import com.matchimban.matchimban_api.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings")
public class VoteController {

    private final VoteService voteService;

    @Operation(summary = "투표 생성")
    @CsrfRequired
    @PostMapping("/{meetingId}/votes")
    public ResponseEntity<CreateVoteResponse> createVote(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ResponseEntity.accepted().body(voteService.createVote(meetingId, principal.memberId()));
    }

    @Operation(summary = "투표 후보 조회")
    @GetMapping("/{meetingId}/votes/{voteId}/candidates")
    public ResponseEntity<VoteCandidatesResponse> getCandidates(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ResponseEntity.ok(voteService.getCandidates(meetingId, voteId, principal.memberId()));
    }

    @Operation(summary = "투표 제출(일괄)")
    @CsrfRequired
    @PostMapping("/{meetingId}/votes/{voteId}/submissions")
    public ResponseEntity<Void> submitVote(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody @Valid VoteSubmitRequest request
    ) {
        voteService.submitVote(meetingId, voteId, principal.memberId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "투표 상태 조회")
    @GetMapping("/{meetingId}/votes/{voteId}/status")
    public ResponseEntity<VoteStatusResponse> getVoteStatus(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ResponseEntity.ok(voteService.getVoteStatus(meetingId, voteId, principal.memberId()));
    }

    @Operation(summary = "투표 결과(Top3) 조회")
    @GetMapping("/{meetingId}/votes/{voteId}/results")
    public ResponseEntity<VoteResultsResponse> getResults(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ResponseEntity.ok(voteService.getResults(meetingId, voteId, principal.memberId()));
    }

    @Operation(summary = "최종 선택")
    @CsrfRequired
    @PostMapping("/{meetingId}/votes/{voteId}/final-selection")
    public ResponseEntity<Void> finalizeSelection(
            @PathVariable Long meetingId,
            @PathVariable Long voteId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody @Valid FinalSelectionRequest request
    ) {
        voteService.finalizeSelection(meetingId, voteId, principal.memberId(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "최종 선택 조회")
    @GetMapping("/{meetingId}/final-selection")
    public ResponseEntity<FinalSelectionResponse> getFinalSelection(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal
    ) {
        return ResponseEntity.ok(voteService.getFinalSelection(meetingId, principal.memberId()));
    }

}
