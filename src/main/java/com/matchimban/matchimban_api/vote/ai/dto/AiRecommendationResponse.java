package com.matchimban.matchimban_api.vote.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
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

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class Restaurant {
        private Long id;
        private String name;

        @JsonProperty("category_mapped")
        private String categoryMapped;

        @JsonProperty("distance_m")
        private Integer distanceM;

        @JsonProperty("final_score")
        private BigDecimal finalScore;

        // FastAPI가 rank를 보낼 수도 있으니 받아두고 싶으면:
         private Integer rank;
    }
}
