package com.matchimban.matchimban_api.event.dto.response;

import com.matchimban.matchimban_api.event.entity.CouponType;
import com.matchimban.matchimban_api.event.entity.EventCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "내 쿠폰 항목")
public record MyCouponItemResponse(
        @Schema(description = "쿠폰 ID")
        Long couponId,
        @Schema(description = "쿠폰명")
        String couponName,
        @Schema(description = "쿠폰 타입")
        CouponType couponType,
        @Schema(description = "이벤트 ID")
        Long eventId,
        @Schema(description = "이벤트명")
        String eventTitle,
        @Schema(description = "발급 시각")
        Instant issuedAt,
        @Schema(description = "만료 시각")
        Instant expiredAt,
        @Schema(description = "사용 시각")
        Instant usedAt,
        @Schema(description = "현재 쿠폰 상태")
        EventCouponStatus status,
        @Schema(description = "현재 사용 가능 여부")
        boolean usable
) {
}
