package com.matchimban.matchimban_api.chat.metrics;

import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ChatMetricsRecorder {

	private static final String SEND_ATTEMPT = "chat.message.send.attempt";
	private static final String SEND_ACCEPTED = "chat.message.send.accepted";
	private static final String MESSAGE_PERSISTED = "chat.message.persisted";
	private static final String READ_POINTER_UPDATE = "chat.read_pointer.update";
	private static final String MESSAGE_CACHE_RECENT_LOOKUP = "chat.cache.messages.recent.lookup";

	private static final String TAG_PATH = "path";
	private static final String TAG_RESULT = "result";
	private static final String TAG_TYPE = "type";

	private final MeterRegistry meterRegistry;

	public ChatMetricsRecorder(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordSendAttempt() {
		meterRegistry.counter(SEND_ATTEMPT).increment();
	}

	public void recordSendAccepted(boolean deduplicated) {
		meterRegistry.counter(SEND_ACCEPTED, TAG_PATH, deduplicated ? "deduplicated" : "created").increment();
	}

	public void recordMessagePersisted(ChatMessageType type) {
		String messageType = type == null ? "unknown" : type.name();
		meterRegistry.counter(MESSAGE_PERSISTED, TAG_TYPE, messageType).increment();
	}

	public void recordReadPointerUpdate(boolean applied) {
		meterRegistry.counter(READ_POINTER_UPDATE, TAG_RESULT, applied ? "applied" : "skipped").increment();
	}

	public void recordMessageRecentCacheLookupHit() {
		meterRegistry.counter(MESSAGE_CACHE_RECENT_LOOKUP, TAG_RESULT, "hit").increment();
	}

	public void recordMessageRecentCacheLookupMiss() {
		meterRegistry.counter(MESSAGE_CACHE_RECENT_LOOKUP, TAG_RESULT, "miss").increment();
	}

	public void recordMessageRecentCacheLookupError() {
		meterRegistry.counter(MESSAGE_CACHE_RECENT_LOOKUP, TAG_RESULT, "error").increment();
	}
}
