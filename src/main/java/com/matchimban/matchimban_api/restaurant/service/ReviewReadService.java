package com.matchimban.matchimban_api.restaurant.service;

import com.matchimban.matchimban_api.restaurant.dto.response.MyReviewsResponse;
import com.matchimban.matchimban_api.restaurant.dto.response.ReviewDetailResponse;

public interface ReviewReadService {
    MyReviewsResponse getMyReviews(Long memberId, Long cursor, int size);
    ReviewDetailResponse getMyReviewDetail(Long memberId, Long reviewId);
}