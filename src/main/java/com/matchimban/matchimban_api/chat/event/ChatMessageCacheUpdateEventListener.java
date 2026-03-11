package com.matchimban.matchimban_api.chat.event;

import com.matchimban.matchimban_api.chat.cache.ChatMessageCacheService;
import com.matchimban.matchimban_api.chat.dto.ChatSenderDto;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageCreatedData;
import com.matchimban.matchimban_api.chat.repository.projection.ChatMessageRow;
import java.time.Instant;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageCacheUpdateEventListener {

	private final ChatMessageCacheService chatMessageCacheService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onChatMessageCreated(ChatMessageCreatedInternalEvent event) {
		try {
			ChatMessageCreatedData data = event.payload().data();
			ChatMessageRow row = toRow(data);
			chatMessageCacheService.appendRecentMessage(data.meetingId(), row);
		} catch (Exception ex) {
			log.warn("Failed to update chat messages cache after commit", ex);
		}
	}

	private ChatMessageRow toRow(ChatMessageCreatedData data) {
		ChatSenderDto sender = data.sender();
		return new ChatMessageRow(
			data.messageId(),
			data.type(),
			data.content(),
			toInstant(data.createdAt()),
			sender == null ? null : sender.userId(),
			sender == null ? null : sender.name(),
			sender == null ? null : sender.profileImageUrl()
		);
	}

	private Instant toInstant(OffsetDateTime createdAt) {
		return createdAt == null ? null : createdAt.toInstant();
	}
}
