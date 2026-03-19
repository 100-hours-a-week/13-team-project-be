package com.matchimban.matchimban_api.event.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Schema(description = "이벤트 목록 조회 요청")
public class EventListRequest {

    @Schema(description = "다음 페이지 조회용 커서 createdAt")
    private Instant cursorCreatedAt;

    @Schema(description = "다음 페이지 조회용 커서 id")
    private Long cursorId;

    @Schema(description = "조회 개수", example = "20", defaultValue = "20")
    private Integer size = 20;

    public int getNormalizedSize() {
        if (size == null || size <= 0) {
            return 20;
        }
        return Math.min(size, 50);
    }

    public boolean hasCursor() {
        return cursorCreatedAt != null && cursorId != null;
    }
}