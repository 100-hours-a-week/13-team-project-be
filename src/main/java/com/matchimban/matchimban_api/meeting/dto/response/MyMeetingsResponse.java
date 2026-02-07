package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "내 모임 목록 조회 응답")
public class MyMeetingsResponse {

    @Schema(description = "모임 목록")
    private List<MyMeetingSummary> items;

    @Schema(description = "다음 페이지 커서")
    private Long nextCursor;

    @Schema(description = "다음 페이지 존재 여부")
    private boolean hasNext;
}