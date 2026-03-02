package com.matchimban.matchimban_api.restaurant.service;

import com.matchimban.matchimban_api.restaurant.dto.request.ReviewCreateRequest;
import com.matchimban.matchimban_api.restaurant.dto.request.ReviewUpdateRequest;
import com.matchimban.matchimban_api.restaurant.dto.response.CreateReviewResponse;
import com.matchimban.matchimban_api.restaurant.dto.response.ReviewDetailResponse;

public interface ReviewService {
    CreateReviewResponse createReview(Long meetingId, Long memberId, ReviewCreateRequest request);
    ReviewDetailResponse updateReview(Long reviewId, Long memberId, ReviewUpdateRequest request);
    void deleteReview(Long reviewId, Long memberId);
}