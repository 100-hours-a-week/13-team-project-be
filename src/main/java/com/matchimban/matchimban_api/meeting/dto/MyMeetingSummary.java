package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "내 모임 목록의 모임 요약 정보")
public class MyMeetingSummary {

    @Schema(description = "모임 ID")
    private Long meetingId;

    @Schema(description = "모임 이름")
    private String title;

    @Schema(description = "모임 일시")
    private LocalDateTime scheduledAt;

    @Schema(description = "모임 주소")
    private String locationAddress;

    @Schema(description = "목표 인원")
    private int targetHeadcount;

    @Schema(description = "현재 참여자 수(ACTIVE 기준)")
    private long participantCount;
}
