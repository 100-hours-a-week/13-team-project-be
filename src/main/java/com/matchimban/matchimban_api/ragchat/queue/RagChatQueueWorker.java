package com.matchimban.matchimban_api.ragchat.queue;

import com.matchimban.matchimban_api.ragchat.client.RagChatProtectedCaller;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskRequest;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskResponse;
import com.matchimban.matchimban_api.ragchat.dto.queue.RagChatQueueMessage;
import com.matchimban.matchimban_api.ragchat.error.RagChatErrorCode;
import com.matchimban.matchimban_api.ragchat.sse.RagChatSseRegistry;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.rabbitmq.client.Channel;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagChatQueueWorker {

	private static final String TIMEOUT_FALLBACK = "응답 시간이 초과됐어요. 다시 시도해주세요.";
	private static final String NO_RESULT_FALLBACK = "조건에 맞는 식당을 찾지 못했어요. 조건을 조금 완화해보세요.";
	private static final String CIRCUIT_OPEN_FALLBACK = "현재 서버가 불안정해요. 잠시 후 다시 시도해주세요.";
	private static final String BULKHEAD_FULL_FALLBACK = "요청이 많아 처리가 지연되고 있어요. 잠시 후 다시 시도해주세요.";

	private final RagChatProtectedCaller protectedCaller;
	private final RagChatSseRegistry sseRegistry;

	@RabbitListener(
		queues = "${rag-chat.queue.name:rag.chat.jobs}",
		ackMode = "MANUAL"
	)
	public void process(
		RagChatQueueMessage message,
		Channel channel,
		@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
	) throws IOException {

		log.info("Processing RAG request. requestId={}", message.requestId());

		try {
			RagEngineAskRequest request = new RagEngineAskRequest(message.userId(), message.question());
			RagEngineAskResponse response = protectedCaller.call(request);

			String answer = normalizeAnswer(response.answer());
			if (answer == null) {
				sseRegistry.complete(message.requestId(), NO_RESULT_FALLBACK);
			} else {
				sseRegistry.complete(message.requestId(), answer);
			}
			channel.basicAck(deliveryTag, false);

		} catch (CallNotPermittedException ex) {
			log.warn("CircuitBreaker OPEN. requestId={}", message.requestId());
			sseRegistry.completeWithError(message.requestId(), CIRCUIT_OPEN_FALLBACK);
			channel.basicAck(deliveryTag, false);

		} catch (BulkheadFullException ex) {
			log.warn("Bulkhead full. requestId={}", message.requestId());
			sseRegistry.completeWithError(message.requestId(), BULKHEAD_FULL_FALLBACK);
			channel.basicAck(deliveryTag, false);

		} catch (ApiException ex) {
			if (ex.getErrorCode() == RagChatErrorCode.RAG_ENGINE_TIMEOUT) {
				sseRegistry.complete(message.requestId(), TIMEOUT_FALLBACK);
				channel.basicAck(deliveryTag, false);
			} else {
				log.error("RAG call failed. requestId={}", message.requestId(), ex);
				sseRegistry.completeWithError(message.requestId(), "처리 중 오류가 발생했어요.");
				channel.basicReject(deliveryTag, false);
			}

		} catch (Exception ex) {
			log.error("Unexpected error. requestId={}", message.requestId(), ex);
			sseRegistry.completeWithError(message.requestId(), "처리 중 오류가 발생했어요.");
			channel.basicReject(deliveryTag, false);
		}
	}

	private String normalizeAnswer(String answer) {
		if (answer == null) {
			return null;
		}
		String trimmed = answer.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
