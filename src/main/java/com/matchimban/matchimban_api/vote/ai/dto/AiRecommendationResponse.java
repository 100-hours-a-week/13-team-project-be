package com.matchimban.matchimban_api.vote.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AiRecommendationResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("top_n")
    private Integer topN;

    private List<Restaurant> restaurants;

    @JsonProperty("created_at")
    @JsonIgnore
    private OffsetDateTime createdAt;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class Restaurant {

        private Long id;

        @JsonProperty("distance_m")
        private Integer distanceM;

        @JsonProperty("final_score")
        private BigDecimal finalScore;

        private Integer rank;
    }
}
