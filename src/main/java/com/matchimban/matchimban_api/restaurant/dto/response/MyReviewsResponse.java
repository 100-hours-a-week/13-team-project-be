package com.matchimban.matchimban_api.restaurant.dto.response;

import com.matchimban.matchimban_api.restaurant.dto.view.MyReviewSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MyReviewsResponse {
    private List<MyReviewSummary> items;
    private Long nextCursor;
    private boolean hasNext;
}