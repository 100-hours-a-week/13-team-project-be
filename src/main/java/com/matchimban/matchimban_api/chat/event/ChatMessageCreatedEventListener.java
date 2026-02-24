package com.matchimban.matchimban_api.chat.event;

import com.matchimban.matchimban_api.chat.redis.ChatRedisPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageCreatedEventListener {

	private final ChatRedisPublisher chatRedisPublisher;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onChatMessageCreated(ChatMessageCreatedInternalEvent event) {
		try {
			chatRedisPublisher.publishMessageCreated(event.payload());
		} catch (Exception ex) {
			log.error("Failed to publish chat message after commit", ex);
		}
	}
}
