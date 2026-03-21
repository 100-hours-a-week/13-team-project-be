package com.matchimban.matchimban_api.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "이벤트 목록 조회 응답")
public class EventListResponse {

    @Schema(description = "이벤트 목록")
    private List<EventSummaryResponse> items;

    @Schema(description = "다음 페이지 존재 여부")
    private boolean hasNext;

    @Schema(description = "다음 페이지 조회용 커서 createdAt")
    private Instant nextCursorCreatedAt;

    @Schema(description = "다음 페이지 조회용 커서 id")
    private Long nextCursorId;
}