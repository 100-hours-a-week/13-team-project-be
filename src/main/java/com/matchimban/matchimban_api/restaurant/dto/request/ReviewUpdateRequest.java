package com.matchimban.matchimban_api.restaurant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {

    @Min(1) @Max(5)
    private int rating;

    @NotBlank
    private String content;
}