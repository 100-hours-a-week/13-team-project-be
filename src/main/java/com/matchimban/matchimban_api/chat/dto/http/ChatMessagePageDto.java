package com.matchimban.matchimban_api.chat.dto.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지 페이지 정보")
public record ChatMessagePageDto(
	@Schema(example = "30")
	int size,
	@JsonProperty("next_cursor")
	@Schema(nullable = true, example = "6650a1b2c3d4e5f678901230")
	String nextCursor,
	@JsonProperty("has_next")
	@Schema(example = "true")
	boolean hasNext
) {
}
