package com.matchimban.matchimban_api.vote.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiRecommendationRequest {
    @JsonProperty("member_id")
    private Long memberId;

    @JsonProperty("request_id")
    private String requestId;

    private Meeting meeting;
    private Location location;
    private Swipe swipe;
    private Preferences preferences;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Meeting {
        @JsonProperty("start_time")
        private String startTime;
        private int headcount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Location {
        private double lat;
        private double lng;
        @JsonProperty("radius_m")
        private int radiusM;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Swipe {
        @JsonProperty("card_limit")
        private int cardLimit;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Preferences {
        private Map<String, Integer> like;
        private Map<String, Integer> dislike;
    }
}
