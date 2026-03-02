package com.matchimban.matchimban_api.restaurant.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.restaurant.dto.request.ReviewCreateRequest;
import com.matchimban.matchimban_api.restaurant.dto.request.ReviewUpdateRequest;
import com.matchimban.matchimban_api.restaurant.dto.response.CreateReviewResponse;
import com.matchimban.matchimban_api.restaurant.dto.response.MyReviewsResponse;
import com.matchimban.matchimban_api.restaurant.dto.response.ReviewDetailResponse;
import com.matchimban.matchimban_api.restaurant.service.ReviewReadService;
import com.matchimban.matchimban_api.restaurant.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewReadService reviewReadService;

    @CsrfRequired
    @PostMapping("/meetings/{meetingId}/reviews")
    public ResponseEntity<CreateReviewResponse> createReview(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        CreateReviewResponse response =
                reviewService.createReview(meetingId, principal.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/member/me/reviews")
    public ResponseEntity<MyReviewsResponse> getMyReviews(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(
                reviewReadService.getMyReviews(principal.memberId(), cursor, size)
        );
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDetailResponse> getReviewDetail(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.ok(
                reviewReadService.getMyReviewDetail(principal.memberId(), reviewId)
        );
    }

    @CsrfRequired
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewDetailResponse> updateReview(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return ResponseEntity.ok(
                reviewService.updateReview(reviewId, principal.memberId(), request)
        );
    }

    @CsrfRequired
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReview(reviewId, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}