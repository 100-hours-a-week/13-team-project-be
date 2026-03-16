package com.matchimban.matchimban_api.ragchat.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RagEngineHistoryMessageItem(
	Long id,
	String role,
	String content,
	@JsonProperty("created_at")
	String createdAt
) {
}
