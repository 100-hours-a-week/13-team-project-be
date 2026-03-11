package com.matchimban.matchimban_api.chat.event;

import com.matchimban.matchimban_api.chat.service.ChatService;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatUnreadCountsRefreshCoalescer {

	private final ChatService chatService;
	private final boolean coalescingEnabled;
	private final long debounceMillis;
	private final ScheduledExecutorService scheduler;
	private final ConcurrentHashMap<Long, MeetingRefreshState> meetingStates = new ConcurrentHashMap<>();

	public ChatUnreadCountsRefreshCoalescer(
		ChatService chatService,
		@Value("${chat.unread.refresh.coalescing-enabled:true}") boolean coalescingEnabled,
		@Value("${chat.unread.refresh.debounce-millis:120}") long debounceMillis,
		@Value("${chat.unread.refresh.coalescing-threads:4}") int coalescingThreads
	) {
		this.chatService = chatService;
		this.coalescingEnabled = coalescingEnabled;
		this.debounceMillis = Math.max(0L, debounceMillis);
		int threadCount = Math.max(1, coalescingThreads);
		AtomicInteger threadSeq = new AtomicInteger(1);
		this.scheduler = Executors.newScheduledThreadPool(threadCount, runnable -> {
			Thread thread = new Thread(
				runnable,
				"chat-unread-refresh-coalescer-" + threadSeq.getAndIncrement()
			);
			thread.setDaemon(true);
			return thread;
		});
	}

	public void request(Long meetingId) {
		if (meetingId == null) {
			return;
		}

		if (!coalescingEnabled) {
			flushNow(meetingId);
			return;
		}

		MeetingRefreshState state = meetingStates.computeIfAbsent(meetingId, key -> new MeetingRefreshState());
		state.dirty.set(true);
		scheduleIfNeeded(meetingId, state);
	}

	private void scheduleIfNeeded(Long meetingId, MeetingRefreshState state) {
		if (!state.scheduled.compareAndSet(false, true)) {
			return;
		}

		scheduler.schedule(() -> flush(meetingId, state), debounceMillis, TimeUnit.MILLISECONDS);
	}

	private void flush(Long meetingId, MeetingRefreshState state) {
		state.scheduled.set(false);
		if (!state.running.compareAndSet(false, true)) {
			scheduleIfNeeded(meetingId, state);
			return;
		}

		try {
			if (state.dirty.getAndSet(false)) {
				flushNow(meetingId);
			}
		} finally {
			state.running.set(false);
			if (state.dirty.get()) {
				scheduleIfNeeded(meetingId, state);
			} else {
				meetingStates.remove(meetingId, state);
			}
		}
	}

	private void flushNow(Long meetingId) {
		try {
			chatService.publishUnreadCountsWindow(meetingId);
		} catch (Exception ex) {
			log.error("Failed to publish unread-counts window. meetingId={}", meetingId, ex);
		}
	}

	@PreDestroy
	public void shutdown() {
		scheduler.shutdownNow();
	}

	private static final class MeetingRefreshState {
		private final AtomicBoolean dirty = new AtomicBoolean(false);
		private final AtomicBoolean scheduled = new AtomicBoolean(false);
		private final AtomicBoolean running = new AtomicBoolean(false);
	}
}
