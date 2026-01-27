package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "모임 참여자 요약 정보")
public class MeetingParticipantSummary {

    @Schema(description = "참여자 memberId")
    private Long memberId;

    @Schema(description = "역할", allowableValues = {"HOST", "MEMBER"})
    private String role;

    @Schema(description = "상태", allowableValues = {"ACTIVE", "INACTIVE", "LEFT"})
    private String status;

    @Schema(description = "참여 일시")
    private LocalDateTime joinedAt;
}
