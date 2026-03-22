package com.matchimban.matchimban_api.ragchat.sse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@Slf4j
public class RagChatSseRegistry {

	private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	public void register(String requestId, SseEmitter emitter) {
		emitters.put(requestId, emitter);
		emitter.onCompletion(() -> emitters.remove(requestId));
		emitter.onTimeout(() -> emitters.remove(requestId));
		emitter.onError(ex -> emitters.remove(requestId));
	}

	public void complete(String requestId, String answer) {
		SseEmitter emitter = emitters.remove(requestId);
		if (emitter == null) {
			log.warn("SSE emitter not found. requestId={}", requestId);
			return;
		}
		try {
			emitter.send(SseEmitter.event()
				.name("answer")
				.data(answer));
			emitter.complete();
		} catch (IOException ex) {
			log.warn("SSE send failed. requestId={}", requestId, ex);
		}
	}

	public void completeWithError(String requestId, String errorMessage) {
		SseEmitter emitter = emitters.remove(requestId);
		if (emitter == null) {
			log.warn("SSE emitter not found for error. requestId={}", requestId);
			return;
		}
		try {
			emitter.send(SseEmitter.event()
				.name("error")
				.data(errorMessage));
			emitter.complete();
		} catch (IOException ex) {
			log.warn("SSE error send failed. requestId={}", requestId, ex);
		}
	}
}
