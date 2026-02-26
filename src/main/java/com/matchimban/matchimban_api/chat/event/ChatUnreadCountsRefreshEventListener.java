package com.matchimban.matchimban_api.chat.event;

import com.matchimban.matchimban_api.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatUnreadCountsRefreshEventListener {

	private final ChatService chatService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUnreadCountsRefresh(ChatUnreadCountsRefreshInternalEvent event) {
		try {
			chatService.publishUnreadCountsWindow(event.meetingId());
		} catch (Exception ex) {
			log.error("Failed to publish unread-counts window after commit. meetingId={}", event.meetingId(), ex);
		}
	}
}
