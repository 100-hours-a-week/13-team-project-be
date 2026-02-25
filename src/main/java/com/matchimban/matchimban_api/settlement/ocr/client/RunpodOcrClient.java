package com.matchimban.matchimban_api.settlement.ocr.client;

import com.matchimban.matchimban_api.settlement.ocr.config.AppOcrProperties;
import com.matchimban.matchimban_api.settlement.ocr.dto.RunpodOcrRequest;
import com.matchimban.matchimban_api.settlement.ocr.dto.RunpodOcrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RunpodOcrClient {

    private final WebClient.Builder webClientBuilder;
    private final AppOcrProperties props;

    public RunpodOcrResponse requestReceiptOcr(String imageUrl, String requestId) {
        WebClient webClient = webClientBuilder
                .baseUrl(trimTrailingSlash(props.getBaseUrl()))
                .build();

        Duration timeout = props.getTimeout();

        return webClient.post()
                .uri("/receipt")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RunpodOcrRequest(imageUrl, requestId))
                .retrieve()
                .bodyToMono(RunpodOcrResponse.class)
                .timeout(timeout)
                .block();
    }

    private String trimTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}