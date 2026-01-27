package com.matchimban.matchimban_api.meeting.controller;

import com.matchimban.matchimban_api.meeting.dto.*;
import com.matchimban.matchimban_api.meeting.service.MeetingReadService;
import com.matchimban.matchimban_api.meeting.service.MeetingService;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    private final MeetingReadService meetingReadService;

    @Operation(summary = "모임 생성", description = "모임 생성하고 inviteCode 반환")
    @ApiResponse(responseCode = "201", description = "created")
    @CsrfRequired
    @PostMapping
    public ResponseEntity<CreateMeetingResponse> createMeeting(
            @RequestParam Long memberId, // TODO: JWT 구현 시 수정
            @Valid @RequestBody CreateMeetingRequest request
    ) {
        CreateMeetingResponse response = meetingService.createMeeting(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "내 모임 목록 조회", description = "참여 중인 모임 목록 조회")
    @GetMapping
    public ResponseEntity<MyMeetingsResponse> getMyMeetings(
            @RequestParam Long memberId, // TODO: JWT 구현 시 수정
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(meetingReadService.getMyMeetings(memberId, cursor, size));
    }

    @Operation(summary = "모임 상세 조회", description = "특정 모임의 상세 정보 조회")
    @GetMapping("/{meetingId}")
    public ResponseEntity<MeetingDetailResponse> getMeetingDetail(
            @RequestParam Long memberId,
            @PathVariable Long meetingId
    ) {
        return ResponseEntity.ok(meetingReadService.getMeetingDetail(memberId, meetingId));
    }

    @Operation(summary = "모임 수정", description = "모임 정보를 부분 수정(호스트만 가능)")
    @CsrfRequired
    @PatchMapping("/{meetingId}")
    public ResponseEntity<UpdateMeetingResponse> updateMeeting(
            @RequestParam Long memberId, // TODO: JWT 구현 시 수정
            @PathVariable Long meetingId,
            @Valid @RequestBody UpdateMeetingRequest request
    ) {
        return ResponseEntity.ok(meetingService.updateMeeting(memberId, meetingId, request));
    }


    @Operation(summary = "모임 삭제", description = "모임 삭제(soft delete), (호스트만 가능)")
    @ApiResponse(responseCode = "204", description = "No Content")
    @CsrfRequired
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Void> deleteMeeting(
            @RequestParam Long memberId, // TODO: JWT 구현 시 수정
            @PathVariable Long meetingId
    ) {
        meetingService.deleteMeeting(memberId, meetingId);
        return ResponseEntity.noContent().build();
    }

}
