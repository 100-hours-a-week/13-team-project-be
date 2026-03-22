package com.matchimban.matchimban_api.ragchat.queue;

import com.matchimban.matchimban_api.ragchat.dto.queue.RagChatQueueMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagChatQueuePublisher {

	private final RabbitTemplate rabbitTemplate;

	@Value("${rag-chat.queue.exchange:rag.chat.exchange}")
	private String exchange;

	@Value("${rag-chat.queue.routing-key:rag.chat.job}")
	private String routingKey;

	public void publish(RagChatQueueMessage message) {
		rabbitTemplate.convertAndSend(exchange, routingKey, message);
		log.info("Published RAG request to queue. requestId={}", message.requestId());
	}
}
