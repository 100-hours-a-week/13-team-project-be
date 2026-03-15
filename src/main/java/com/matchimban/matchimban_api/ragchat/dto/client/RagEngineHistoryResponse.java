package com.matchimban.matchimban_api.ragchat.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RagEngineHistoryResponse(
	List<RagEngineHistoryMessageItem> messages,
	@JsonProperty("next_cursor")
	Long nextCursor
) {
}
