package com.matchimban.matchimban_api.vote.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiRecommendationResponse {
    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("member_id")
    private Long memberId;

    @JsonProperty("top_n")
    private int topN;

    private List<Restaurant> restaurants;

    @JsonProperty("created_at")
    private String createdAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Restaurant {
        private int rank;
        @JsonProperty("store_id")
        private Long storeId;
        @JsonProperty("distance_m")
        private int distanceM;
        @JsonProperty("final_score")
        private BigDecimal finalScore;
    }
}
