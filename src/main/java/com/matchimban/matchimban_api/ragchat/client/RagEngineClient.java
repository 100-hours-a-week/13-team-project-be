package com.matchimban.matchimban_api.ragchat.client;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskRequest;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskResponse;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineHealthResponse;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineHistoryResponse;
import com.matchimban.matchimban_api.ragchat.error.RagChatErrorCode;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagEngineClient {

	private final WebClient ragChatWebClient;

	@Value("${rag-chat.timeout-ms:10000}")
	private long timeoutMillis;

	@Value("${rag-chat.paths.ask:/RAGchat}")
	private String askPath;

	@Value("${rag-chat.paths.reset:/RAGchat/{user_id}}")
	private String resetPath;

	@Value("${rag-chat.paths.history:/history/{user_id}}")
	private String historyPath;

	@Value("${rag-chat.paths.health:/health}")
	private String healthPath;

	public RagEngineAskResponse ask(RagEngineAskRequest request) {
		Mono<RagEngineAskResponse> mono = ragChatWebClient.post()
			.uri(askPath)
			.bodyValue(request)
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_BAD_REQUEST)
			)
			.onStatus(HttpStatusCode::is5xxServerError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_UNAVAILABLE)
			)
			.bodyToMono(RagEngineAskResponse.class);

		return executeWithBody("ask", mono);
	}

	public void resetHistory(String userId) {
		Mono<Void> mono = ragChatWebClient.delete()
			.uri(resetPath, Map.of("user_id", userId))
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_BAD_REQUEST)
			)
			.onStatus(HttpStatusCode::is5xxServerError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_UNAVAILABLE)
			)
			.bodyToMono(Void.class);

		executeWithoutBody("reset_history", mono);
	}

	public RagEngineHistoryResponse getHistory(String userId, int limit, Long beforeId) {
		Mono<RagEngineHistoryResponse> mono = ragChatWebClient.get()
			.uri(uriBuilder -> uriBuilder
				.path(historyPath)
				.queryParam("limit", limit)
				.queryParamIfPresent("before_id", Optional.ofNullable(beforeId))
				.build(Map.of("user_id", userId))
			)
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_BAD_REQUEST)
			)
			.onStatus(HttpStatusCode::is5xxServerError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_UNAVAILABLE)
			)
			.bodyToMono(RagEngineHistoryResponse.class);

		return executeWithBody("get_history", mono);
	}

	public RagEngineHealthResponse health() {
		Mono<RagEngineHealthResponse> mono = ragChatWebClient.get()
			.uri(healthPath)
			.retrieve()
			.onStatus(HttpStatusCode::is4xxClientError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_BAD_REQUEST)
			)
			.onStatus(HttpStatusCode::is5xxServerError,
				response -> toApiException(response, RagChatErrorCode.RAG_ENGINE_UNAVAILABLE)
			)
			.bodyToMono(RagEngineHealthResponse.class);

		return executeWithBody("health", mono);
	}

	private <T> T executeWithBody(String operation, Mono<T> mono) {
		try {
			T response = mono.timeout(timeoutDuration()).block();
			if (response == null) {
				throw new ApiException(RagChatErrorCode.RAG_ENGINE_RESPONSE_INVALID);
			}
			return response;
		} catch (ApiException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw mapRuntimeException(operation, ex);
		}
	}

	private void executeWithoutBody(String operation, Mono<Void> mono) {
		try {
			mono.timeout(timeoutDuration()).block();
		} catch (ApiException ex) {
			throw ex;
		} catch (RuntimeException ex) {
			throw mapRuntimeException(operation, ex);
		}
	}

	private Duration timeoutDuration() {
		long sanitized = Math.max(1000L, timeoutMillis);
		return Duration.ofMillis(sanitized);
	}

	private RuntimeException mapRuntimeException(String operation, RuntimeException ex) {
		if (isTimeoutException(ex)) {
			return new ApiException(RagChatErrorCode.RAG_ENGINE_TIMEOUT);
		}
		if (isDecodingException(ex)) {
			return new ApiException(RagChatErrorCode.RAG_ENGINE_RESPONSE_INVALID, ex.getMessage());
		}
		log.warn("RAG engine call failed. operation={}, reason={}", operation, ex.getMessage());
		return new ApiException(RagChatErrorCode.RAG_ENGINE_UNAVAILABLE, ex.getMessage());
	}

	private boolean isTimeoutException(Throwable throwable) {
		return hasCause(throwable, TimeoutException.class) || hasCauseNameContaining(throwable, "ReadTimeoutException");
	}

	private boolean isDecodingException(Throwable throwable) {
		return hasCauseNameContaining(throwable, "DecodingException")
			|| hasCauseNameContaining(throwable, "JsonProcessingException")
			|| hasCauseNameContaining(throwable, "MismatchedInputException");
	}

	private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
		Throwable current = throwable;
		while (current != null) {
			if (type.isInstance(current)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private boolean hasCauseNameContaining(Throwable throwable, String token) {
		Throwable current = throwable;
		while (current != null) {
			if (current.getClass().getSimpleName().contains(token)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private Mono<? extends Throwable> toApiException(ClientResponse response, RagChatErrorCode errorCode) {
		return response.bodyToMono(String.class)
			.defaultIfEmpty("")
			.map(body -> new ApiException(errorCode, body));
	}
}
