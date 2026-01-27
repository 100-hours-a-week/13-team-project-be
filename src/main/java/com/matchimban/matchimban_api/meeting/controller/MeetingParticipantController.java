package com.matchimban.matchimban_api.meeting.controller;

import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingResponse;
import com.matchimban.matchimban_api.meeting.service.MeetingParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MeetingParticipate", description = "모임 참여 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MeetingParticipantController {

    private final MeetingParticipationService meetingParticipationService;

    @Operation(summary = "모임 참여", description = "inviteCode로 모임에 참여")
    @PostMapping("/participate_meetings")
    public ResponseEntity<ParticipateMeetingResponse> participateMeeting(
            @RequestParam Long memberId, //TODO: JWT 구현 시 수정
            @Valid @RequestBody ParticipateMeetingRequest request
    ) {
        ParticipateMeetingResponse response = meetingParticipationService.participateMeeting(memberId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "모임 탈퇴", description = "현재 사용자가 모임 탈퇴(호스트는 불가)")
    @ApiResponse(responseCode = "204", description = "No Content")
    @DeleteMapping("meetings/{meetingId}/members/me")
    public ResponseEntity<Void> leaveMeeting(
            @RequestParam Long memberId, // TODO: JWT로 구현 시 수정
            @PathVariable Long meetingId
    ) {
        meetingParticipationService.leaveMeeting(memberId, meetingId);
        return ResponseEntity.noContent().build();
    }

}