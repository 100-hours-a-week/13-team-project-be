package com.matchimban.matchimban_api.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewDetailResponse {
    private Long reviewId;
    private Long meetingId;

    private int rating;
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long restaurantId;
    private String restaurantName;
    private String restaurantImageUrl1;

    private String categoryName;
    private String categoryEmoji;
}