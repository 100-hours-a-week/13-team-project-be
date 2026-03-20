package com.matchimban.matchimban_api.chat.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatUnreadCountsRefreshEventListener {

	private final ChatUnreadCountsRefreshCoalescer unreadCountsRefreshCoalescer;

	@Async
	@EventListener
	public void onUnreadCountsRefresh(ChatUnreadCountsRefreshInternalEvent event) {
		try {
			unreadCountsRefreshCoalescer.request(event.meetingId());
		} catch (Exception ex) {
			log.error("Failed to enqueue unread-counts refresh after commit. meetingId={}", event.meetingId(), ex);
		}
	}
}
