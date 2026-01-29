package com.matchimban.matchimban_api.vote.ai;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class RecommendationClientConfig {

    @Bean
    public WebClient recommendationWebClient(
            @Value("${ai-recommendation.base-url}") String baseUrl
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
