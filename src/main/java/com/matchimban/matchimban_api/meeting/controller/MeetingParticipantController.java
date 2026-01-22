package com.matchimban.matchimban_api.meeting.controller;

import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingResponse;
import com.matchimban.matchimban_api.meeting.service.MeetingParticipationService;
import com.matchimban.matchimban_api.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
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

}
