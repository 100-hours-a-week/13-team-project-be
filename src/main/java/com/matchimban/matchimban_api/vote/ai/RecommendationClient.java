package com.matchimban.matchimban_api.vote.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.vote.ai.dto.AiRecommendationRequest;
import com.matchimban.matchimban_api.vote.ai.dto.AiRecommendationResponse;
import java.time.Duration;

import com.matchimban.matchimban_api.vote.error.VoteErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationClient {

    private final WebClient recommendationWebClient;
    private final ObjectMapper objectMapper;

    @Value("${ai-recommendation.timeout-ms:5000}")
    private long timeoutMs;

    public AiRecommendationResponse recommend(AiRecommendationRequest request) {
        return recommendationWebClient.post()
                .uri("/recommendations")
                .bodyValue(request)
                .retrieve()

                .onStatus(status -> status.isError(), resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .map(body -> {
                                    HttpStatus s = HttpStatus.valueOf(resp.statusCode().value());
                                    VoteErrorCode ec = mapAiStatusToErrorCode(s);
                                    return new ApiException(ec, body);
                                })
                )

                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .map(raw -> {
                    log.info("[AI RAW RESP] {}", raw);

                    try {
                        AiRecommendationResponse parsed =
                                objectMapper.readValue(raw, AiRecommendationResponse.class);

                        int size = (parsed.getRestaurants() == null) ? 0 : parsed.getRestaurants().size();
                        Long firstId = (size == 0) ? null : parsed.getRestaurants().get(0).getId();

                        log.info("[AI PARSED RESP] requestId={}, topN={}, restaurants.size={}, firstId={}",
                                parsed.getRequestId(), parsed.getTopN(), size, firstId);

                        return parsed;
                    } catch (Exception e) {
                        throw new ApiException(VoteErrorCode.AI_RESPONSE_INVALID,
                                "Failed to parse AI response: " + e.getMessage() + " | raw=" + raw);
                    }
                })
                .block();
    }

    private VoteErrorCode mapAiStatusToErrorCode(HttpStatus aiStatus) {
        return switch (aiStatus) {
            case NOT_FOUND -> VoteErrorCode.NO_RESTAURANTS_FOUND;
            case SERVICE_UNAVAILABLE -> VoteErrorCode.AI_RECOMMENDATION_FAILED;
            case INTERNAL_SERVER_ERROR -> VoteErrorCode.AI_RECOMMENDATION_FAILED;
            case BAD_REQUEST -> VoteErrorCode.AI_RECOMMENDATION_FAILED;
            case UNPROCESSABLE_ENTITY -> VoteErrorCode.AI_RECOMMENDATION_FAILED;
            default -> VoteErrorCode.AI_RECOMMENDATION_FAILED;
        };
    }
}
