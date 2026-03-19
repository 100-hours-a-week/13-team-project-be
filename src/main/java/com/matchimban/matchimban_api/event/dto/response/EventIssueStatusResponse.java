package com.matchimban.matchimban_api.event.dto.response;

import com.matchimban.matchimban_api.event.entity.EventIssueFailureReason;
import com.matchimban.matchimban_api.event.entity.EventIssueRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "이벤트 쿠폰 발급 상태 응답")
public record EventIssueStatusResponse(
        @Schema(description = "요청 ID")
        String requestId,
        @Schema(description = "현재 요청 상태")
        EventIssueRequestStatus status,
        @Schema(description = "실패 사유")
        EventIssueFailureReason reason,
        @Schema(description = "현재 대기 순번")
        Long queuePosition,
        @Schema(description = "발급 성공 쿠폰 ID")
        Long couponId,
        @Schema(description = "발급 시각")
        Instant issuedAt,
        @Schema(description = "만료 시각")
        Instant expiredAt
) {
}
