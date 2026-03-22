package com.matchimban.matchimban_api.ragchat.client;

import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskRequest;
import com.matchimban.matchimban_api.ragchat.dto.client.RagEngineAskResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagChatProtectedCaller {

	private final RagEngineClient ragEngineClient;

	@CircuitBreaker(name = "rag-chat")
	@Bulkhead(name = "rag-chat")
	public RagEngineAskResponse call(RagEngineAskRequest request) {
		return ragEngineClient.ask(request);
	}
}
