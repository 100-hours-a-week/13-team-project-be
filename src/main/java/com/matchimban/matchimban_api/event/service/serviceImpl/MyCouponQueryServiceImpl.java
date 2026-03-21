package com.matchimban.matchimban_api.event.service.serviceImpl;

import com.matchimban.matchimban_api.event.dto.request.MyCouponListRequest;
import com.matchimban.matchimban_api.event.dto.response.MyCouponItemResponse;
import com.matchimban.matchimban_api.event.dto.response.MyCouponListResponse;
import com.matchimban.matchimban_api.event.entity.EventCoupon;
import com.matchimban.matchimban_api.event.entity.EventCouponStatus;
import com.matchimban.matchimban_api.event.repository.EventCouponRepository;
import com.matchimban.matchimban_api.event.service.MyCouponQueryService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyCouponQueryServiceImpl implements MyCouponQueryService {

    private static final Instant INITIAL_CURSOR_CREATED_AT = Instant.parse("9999-12-31T23:59:59Z");
    private static final Long INITIAL_CURSOR_ID = Long.MAX_VALUE;

    private final EventCouponRepository eventCouponRepository;

    @Override
    public MyCouponListResponse getMyCoupons(Long memberId, MyCouponListRequest request) {
        int size = request.getNormalizedSize();
        Instant cursorCreatedAt = request.getCursorCreatedAt() == null
                ? INITIAL_CURSOR_CREATED_AT
                : request.getCursorCreatedAt();
        Long cursorId = request.getCursorId() == null
                ? INITIAL_CURSOR_ID
                : request.getCursorId();
        Instant now = Instant.now();

        List<EventCoupon> coupons = eventCouponRepository.findMyCouponPage(
                memberId,
                cursorCreatedAt,
                cursorId,
                PageRequest.of(0, size + 1)
        );

        boolean hasNext = coupons.size() > size;
        List<EventCoupon> pageItems = hasNext ? coupons.subList(0, size) : coupons;

        Instant nextCursorCreatedAt = null;
        Long nextCursorId = null;
        if (hasNext && !pageItems.isEmpty()) {
            EventCoupon last = pageItems.get(pageItems.size() - 1);
            nextCursorCreatedAt = last.getCreatedAt();
            nextCursorId = last.getId();
        }

        List<MyCouponItemResponse> items = pageItems.stream()
                .map(coupon -> toResponse(coupon, now))
                .toList();

        return new MyCouponListResponse(items, hasNext, nextCursorCreatedAt, nextCursorId);
    }

    private MyCouponItemResponse toResponse(EventCoupon coupon, Instant now) {
        EventCouponStatus status = resolveStatus(coupon, now);
        return new MyCouponItemResponse(
                coupon.getId(),
                coupon.getCouponType().getDisplayName(),
                coupon.getCouponType(),
                coupon.getEventParticipant().getEvent().getId(),
                coupon.getEventParticipant().getEvent().getTitle(),
                coupon.getCreatedAt(),
                coupon.getExpiredAt(),
                coupon.getUsedAt(),
                status,
                status == EventCouponStatus.ISSUED
        );
    }

    private EventCouponStatus resolveStatus(EventCoupon coupon, Instant now) {
        if (coupon.getStatus() == EventCouponStatus.USED) {
            return EventCouponStatus.USED;
        }
        if (coupon.getStatus() == EventCouponStatus.EXPIRED || !coupon.getExpiredAt().isAfter(now)) {
            return EventCouponStatus.EXPIRED;
        }
        return EventCouponStatus.ISSUED;
    }
}
