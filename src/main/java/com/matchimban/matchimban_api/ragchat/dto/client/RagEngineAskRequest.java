package com.matchimban.matchimban_api.ragchat.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagEngineAskRequest(
	@JsonProperty("user_id")
	String userId,
	String message
) {
}
