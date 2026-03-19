package com.matchimban.matchimban_api.event.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.event.dto.request.MyCouponListRequest;
import com.matchimban.matchimban_api.event.dto.response.MyCouponListResponse;
import com.matchimban.matchimban_api.event.service.MyCouponQueryService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Event", description = "이벤트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member/me/coupons")
public class MyCouponController {

    private final MyCouponQueryService myCouponQueryService;

    @Operation(summary = "내 쿠폰 목록 조회", description = "마이페이지에서 로그인한 회원의 쿠폰 목록을 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResult<MyCouponListResponse>> getMyCoupons(
            @AuthenticationPrincipal MemberPrincipal principal,
            MyCouponListRequest request
    ) {
        MyCouponListResponse response = myCouponQueryService.getMyCoupons(principal.memberId(), request);
        return ResponseEntity.ok(ApiResult.of("my_coupons_loaded", response));
    }
}
