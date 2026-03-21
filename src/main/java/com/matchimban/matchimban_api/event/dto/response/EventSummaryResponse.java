package com.matchimban.matchimban_api.event.dto.response;

import com.matchimban.matchimban_api.event.entity.CouponType;
import com.matchimban.matchimban_api.event.entity.EventProgressStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
@Schema(description = "이벤트 요약 응답")
public class EventSummaryResponse {

    @Schema(description = "이벤트 ID")
    private Long eventId;

    @Schema(description = "이벤트 제목")
    private String title;

    @Schema(description = "이벤트 설명")
    private String description;

    @Schema(description = "쿠폰 타입")
    private CouponType couponType;

    @Schema(description = "이벤트 진행 상태")
    private EventProgressStatus progressStatus;

    @Schema(description = "이벤트 시작 시간")
    private Instant startAt;

    @Schema(description = "이벤트 종료 시간")
    private Instant endAt;

    @Schema(description = "총 발급 가능 수량")
    private int capacity;

    @Schema(description = "현재 발급 수량")
    private int issuedCount;

    @Schema(description = "남은 수량")
    private int remainingCount;
}