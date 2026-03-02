package com.matchimban.matchimban_api.restaurant.dto.view;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyReviewSummary {
    private Long reviewId;
    private Long meetingId;

    private int rating;
    private String content;
    private LocalDateTime createdAt;

    private Long restaurantId;
    private String restaurantName;
    private String restaurantImageUrl1;

    private String categoryName;
    private String categoryEmoji;
}