package com.matchimban.matchimban_api.ragchat.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagEngineAskResponse(
	String answer,
	@JsonProperty("user_id")
	String userId
) {
}
