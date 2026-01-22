package com.matchimban.matchimban_api.meeting.controller;

import com.matchimban.matchimban_api.meeting.dto.CreateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingResponse;
import com.matchimban.matchimban_api.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Meeting", description = "모임 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    @Operation(summary = "모임 생성", description = "모임 생성하고 inviteCode 반환")
    @ApiResponse(responseCode = "201", description = "created")
    @PostMapping
    public ResponseEntity<CreateMeetingResponse> createMeeting(
            @RequestParam Long memberId, // TODO: JWT 구현 시 수정
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        CreateMeetingResponse response = meetingService.createMeeting(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
