package com.matchimban.matchimban_api.restaurant.repository.projection;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class MyReviewRow {
    private Long reviewId;
    private Long meetingId;

    private int rating;
    private String content;
    private Instant createdAt;

    private Long restaurantId;
    private String restaurantName;
    private String restaurantImageUrl1;

    private String categoryName;
    private String categoryEmoji;
}