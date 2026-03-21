package com.matchimban.matchimban_api.event.service;

import com.matchimban.matchimban_api.event.entity.CouponType;
import com.matchimban.matchimban_api.event.entity.EventCoupon;
import java.time.Instant;
import java.util.List;

public interface EventCouponUseService {

    int countAvailableCoupons(Long memberId, CouponType couponType, Instant now);

    List<EventCoupon> claimCoupons(Long memberId, CouponType couponType, int count, Instant now);
}
