package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "내 모임 목록 카드(요약)")
public class MyMeetingSummary {

    @Schema(description = "모임 ID")
    private Long meetingId;

    @Schema(description = "모임 제목")
    private String title;

    @Schema(description = "모임 일시")
    private LocalDateTime scheduledAt;

    @Schema(description = "현재 참여자 수")
    private Long participantCount;

    @Schema(description = "목표 인원")
    private Integer targetHeadcount;

    @Schema(description = "모임 상태")
    private MeetingStatus meetingStatus;
}