package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "unread_count 계산 기준 윈도우")
public record ChatUnreadCountsBasis(
	@JsonProperty("window_size")
	@Schema(example = "50")
	int windowSize,
	@JsonProperty("from_message_id")
	@Schema(nullable = true, example = "6650a1b2c3d4e5f678901200")
	String fromMessageId,
	@JsonProperty("to_message_id")
	@Schema(nullable = true, example = "6650a1b2c3d4e5f678901234")
	String toMessageId
) {
}
