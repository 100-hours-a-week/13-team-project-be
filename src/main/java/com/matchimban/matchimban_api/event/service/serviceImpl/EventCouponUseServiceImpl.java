package com.matchimban.matchimban_api.event.service.serviceImpl;

import com.matchimban.matchimban_api.event.entity.CouponType;
import com.matchimban.matchimban_api.event.entity.EventCoupon;
import com.matchimban.matchimban_api.event.repository.EventCouponRepository;
import com.matchimban.matchimban_api.event.service.EventCouponUseService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCouponUseServiceImpl implements EventCouponUseService {

    private final EventCouponRepository eventCouponRepository;

    @Override
    @Transactional(readOnly = true)
    public int countAvailableCoupons(Long memberId, CouponType couponType, Instant now) {
        return Math.toIntExact(eventCouponRepository.countUsableCoupons(memberId, couponType, now));
    }

    @Override
    @Transactional
    public List<EventCoupon> claimCoupons(Long memberId, CouponType couponType, int count, Instant now) {
        if (count <= 0) {
            return List.of();
        }

        return eventCouponRepository.findUsableCouponsForUpdate(
                memberId,
                couponType,
                now,
                PageRequest.of(0, count)
        );
    }
}
