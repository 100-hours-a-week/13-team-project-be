package com.matchimban.matchimban_api.vote.ai;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.vote.ai.dto.AiRecommendationRequest;
import com.matchimban.matchimban_api.vote.ai.dto.AiRecommendationResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class RecommendationClient {

    private final WebClient recommendationWebClient;

    @Value("${ai-recommendation.timeout-ms:5000}")
    private long timeoutMs;

    public AiRecommendationResponse recommend(AiRecommendationRequest request) {
        return recommendationWebClient.post()
                .uri("/api/v1/recommendations")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> {
                                    HttpStatus s = HttpStatus.valueOf(resp.statusCode().value());
                                    String msg = mapStatusToMessage(s);
                                    return new ApiException(mapStatusToHttpStatusForBe(s), msg, body);
                                })
                )

                .bodyToMono(AiRecommendationResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
    }

    private String mapStatusToMessage(HttpStatus aiStatus) {
        return switch (aiStatus) {
            case BAD_REQUEST -> "invalid_request";
            case UNPROCESSABLE_ENTITY -> "unprocessable_preferences";
            case NOT_FOUND -> "no_restaurants_found";
            case INTERNAL_SERVER_ERROR -> "recommendation_failed";
            case SERVICE_UNAVAILABLE -> "database_unavailable";
            default -> "recommendation_failed";
        };
    }

    private HttpStatus mapStatusToHttpStatusForBe(HttpStatus aiStatus) {
        return switch (aiStatus) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNPROCESSABLE_ENTITY -> HttpStatus.UNPROCESSABLE_ENTITY;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }
}
