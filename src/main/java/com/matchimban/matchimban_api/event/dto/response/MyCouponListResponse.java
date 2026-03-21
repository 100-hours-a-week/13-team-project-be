package com.matchimban.matchimban_api.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "내 쿠폰 목록 조회 응답")
public record MyCouponListResponse(
        @Schema(description = "쿠폰 목록")
        List<MyCouponItemResponse> items,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,
        @Schema(description = "다음 페이지 조회용 커서 createdAt")
        Instant nextCursorCreatedAt,
        @Schema(description = "다음 페이지 조회용 커서 id")
        Long nextCursorId
) {
}
