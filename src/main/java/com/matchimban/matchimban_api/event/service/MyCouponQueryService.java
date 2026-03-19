package com.matchimban.matchimban_api.event.service;

import com.matchimban.matchimban_api.event.dto.request.MyCouponListRequest;
import com.matchimban.matchimban_api.event.dto.response.MyCouponListResponse;

public interface MyCouponQueryService {

    MyCouponListResponse getMyCoupons(Long memberId, MyCouponListRequest request);
}
