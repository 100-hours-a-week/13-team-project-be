package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "unread_count 계산 기준 윈도우")
public record ChatUnreadCountsBasis(
	@JsonProperty("window_size")
	@Schema(example = "50")
	int windowSize,
	@JsonProperty("from_message_id")
	@Schema(nullable = true, example = "12954")
	Long fromMessageId,
	@JsonProperty("to_message_id")
	@Schema(nullable = true, example = "13003")
	Long toMessageId
) {
}
