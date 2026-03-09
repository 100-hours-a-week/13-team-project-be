package com.matchimban.matchimban_api.settlement.ocr.client;

import com.matchimban.matchimban_api.settlement.ocr.config.AppOcrProperties;
import com.matchimban.matchimban_api.settlement.ocr.dto.RunpodOcrRequest;
import com.matchimban.matchimban_api.settlement.ocr.dto.RunpodOcrResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RunpodOcrClient {

    private final WebClient.Builder webClientBuilder;
    private final AppOcrProperties props;

    public void assertHealthy() {
        WebClient webClient = webClientBuilder
                .baseUrl(trimTrailingSlash(props.getBaseUrl()))
                .build();

        try {
            Boolean healthy = webClient.get()
                    .uri("/health")
                    .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                    .timeout(props.getHealthTimeout())
                    .block();

            if (healthy == null || !healthy) {
                throw new OcrClientException(
                        "RUNPOD_NOT_READY",
                        "OCR 서버가 아직 준비되지 않았습니다.",
                        true
                );
            }
        } catch (OcrClientException e) {
            throw e;
        } catch (Exception e) {
            throw new OcrClientException(
                    "RUNPOD_HEALTH_CHECK_FAILED",
                    safeMessage(e),
                    true
            );
        }
    }

    public RunpodOcrResponse requestReceiptOcr(String imageUrl, String requestId) {
        WebClient webClient = webClientBuilder
                .baseUrl(trimTrailingSlash(props.getBaseUrl()))
                .build();

        try {
            RunpodOcrResponse response = webClient.post()
                    .uri("/receipt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new RunpodOcrRequest(imageUrl, requestId))
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().is2xxSuccessful()) {
                            return clientResponse.bodyToMono(RunpodOcrResponse.class);
                        }

                        return clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(mapHttpError(clientResponse.rawStatusCode(), body)));
                    })
                    .timeout(props.getTimeout())
                    .block();

            if (response == null) {
                throw new OcrClientException(
                        "RUNPOD_EMPTY_RESPONSE",
                        "OCR 서버 응답이 비어 있습니다.",
                        false
                );
            }

            return response;
        } catch (OcrClientException e) {
            throw e;
        } catch (Exception e) {
            throw mapTransportError(e);
        }
    }

    private OcrClientException mapHttpError(int status, String body) {
        String message = (body == null || body.isBlank()) ? "OCR HTTP 오류" : body;

        if (status == 429 || status >= 500) {
            return new OcrClientException(
                    "RUNPOD_HTTP_" + status,
                    message,
                    true
            );
        }

        if (status == 422) {
            return new OcrClientException(
                    "RUNPOD_HTTP_422",
                    message,
                    false
            );
        }

        return new OcrClientException(
                "RUNPOD_HTTP_" + status,
                message,
                false
        );
    }

    private OcrClientException mapTransportError(Exception e) {
        String message = safeMessage(e);

        if (containsAny(message,
                "Connection reset",
                "Connection refused",
                "ReadTimeout",
                "ConnectTimeout",
                "TimeoutException",
                "timed out")) {
            return new OcrClientException(
                    "RUNPOD_NETWORK",
                    message,
                    true
            );
        }

        return new OcrClientException(
                "RUNPOD_CLIENT_ERROR",
                message,
                false
        );
    }

    private boolean containsAny(String source, String... keywords) {
        if (source == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safeMessage(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private String trimTrailingSlash(String s) {
        if (s == null) return null;
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}