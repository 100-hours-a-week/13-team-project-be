package com.matchimban.matchimban_api.event.dto.response;

import com.matchimban.matchimban_api.event.entity.EventIssueFailureReason;
import com.matchimban.matchimban_api.event.entity.EventIssueRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이벤트 쿠폰 발급 요청 응답")
public record EventIssueRequestResponse(
        @Schema(description = "요청 ID")
        String requestId,
        @Schema(description = "현재 요청 상태")
        EventIssueRequestStatus status,
        @Schema(description = "실패 또는 중복 요청 사유")
        EventIssueFailureReason reason,
        @Schema(description = "현재 대기 순번")
        Long queuePosition,
        @Schema(description = "권장 polling 주기(ms)")
        long pollingIntervalMillis
) {
}
