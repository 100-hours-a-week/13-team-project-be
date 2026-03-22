package com.matchimban.matchimban_api.ragchat.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.ragchat.client.RagChatProtectedCaller;
import com.matchimban.matchimban_api.ragchat.client.RagEngineClient;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskRequest;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskResponse;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineHealthResponse;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineHistoryMessageItem;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineHistoryResponse;
import com.matchimban.matchimban_api.ragchat.dto.request.RagChatAskRequest;
import com.matchimban.matchimban_api.ragchat.dto.response.RagChatAskData;
import com.matchimban.matchimban_api.ragchat.dto.response.RagHealthData;
import com.matchimban.matchimban_api.ragchat.dto.response.RagHistoryData;
import com.matchimban.matchimban_api.ragchat.dto.response.RagHistoryMessageItem;
import com.matchimban.matchimban_api.ragchat.error.RagChatErrorCode;
import com.matchimban.matchimban_api.ragchat.service.RagChatService;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatServiceImpl implements RagChatService {

	private static final String USER_ID_PREFIX = "member:";
	private static final String TIMEOUT_FALLBACK_ANSWER = "응답 시간이 초과됐어요. 다시 시도해주세요.";
	private static final String NO_RESULT_FALLBACK_ANSWER = "조건에 맞는 식당을 찾지 못했어요. 조건을 조금 완화해보세요.";
	private static final String CIRCUIT_OPEN_FALLBACK_ANSWER = "현재 서버가 불안정해요. 잠시 후 다시 시도해주세요.";
	private static final String BULKHEAD_FULL_FALLBACK_ANSWER = "요청이 많아 처리가 지연되고 있어요. 잠시 후 다시 시도해주세요.";

	private final RagChatProtectedCaller protectedCaller;
	private final RagEngineClient ragEngineClient;

	@Override
	public RagChatAskData ask(Long memberId, RagChatAskRequest request) {
		assertUserIdOwner(memberId, request.userId());
		RagEngineAskRequest engineRequest = new RagEngineAskRequest(request.userId(), request.message());

		try {
			RagEngineAskResponse engineResponse = protectedCaller.call(engineRequest);
			String answer = normalizeAnswer(engineResponse.answer());
			if (answer == null) {
				answer = NO_RESULT_FALLBACK_ANSWER;
			}
			return new RagChatAskData(answer, request.userId());
		} catch (CallNotPermittedException ex) {
			log.warn("CircuitBreaker OPEN for rag-chat. memberId={}", memberId);
			return new RagChatAskData(CIRCUIT_OPEN_FALLBACK_ANSWER, request.userId());
		} catch (BulkheadFullException ex) {
			log.warn("Bulkhead full for rag-chat. memberId={}", memberId);
			return new RagChatAskData(BULKHEAD_FULL_FALLBACK_ANSWER, request.userId());
		} catch (ApiException ex) {
			if (ex.getErrorCode() == RagChatErrorCode.RAG_ENGINE_TIMEOUT) {
				return new RagChatAskData(TIMEOUT_FALLBACK_ANSWER, request.userId());
			}
			throw ex;
		} catch (RuntimeException ex) {
			log.error("Unexpected rag ask error. memberId={}, userId={}", memberId, request.userId(), ex);
			throw new ApiException(RagChatErrorCode.RAG_ENGINE_FAILED);
		}
	}

	@Override
	@Transactional
	public void resetHistory(Long memberId, String userId) {
		assertUserIdOwner(memberId, userId);
		ragEngineClient.resetHistory(userId);
	}

	@Override
	public RagHistoryData getHistory(Long memberId, String userId, int limit, Long beforeId) {
		assertUserIdOwner(memberId, userId);
		RagEngineHistoryResponse engineResponse = ragEngineClient.getHistory(userId, limit, beforeId);
		List<RagHistoryMessageItem> messages = mapMessages(engineResponse.messages());
		return new RagHistoryData(messages, engineResponse.nextCursor());
	}

	@Override
	public RagHealthData health() {
		RagEngineHealthResponse response = ragEngineClient.health();
		String status = response.status() == null || response.status().isBlank()
			? "unknown"
			: response.status().trim();
		return new RagHealthData(status);
	}

	private List<RagHistoryMessageItem> mapMessages(List<RagEngineHistoryMessageItem> messages) {
		if (messages == null || messages.isEmpty()) {
			return List.of();
		}
		return messages.stream()
			.map(message -> new RagHistoryMessageItem(
				message.id(),
				message.role(),
				message.content(),
				message.createdAt()
			))
			.toList();
	}

	@Override
	public void assertOwner(Long memberId, String userId) {
		assertUserIdOwner(memberId, userId);
	}

	private void assertUserIdOwner(Long memberId, String requestedUserId) {
		String expected = toCanonicalUserId(memberId);
		if (!expected.equals(requestedUserId)) {
			throw new ApiException(RagChatErrorCode.FORBIDDEN_USER_ID);
		}
	}

	private String toCanonicalUserId(Long memberId) {
		return USER_ID_PREFIX + memberId;
	}

	private String normalizeAnswer(String answer) {
		if (answer == null) {
			return null;
		}
		String trimmed = answer.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
