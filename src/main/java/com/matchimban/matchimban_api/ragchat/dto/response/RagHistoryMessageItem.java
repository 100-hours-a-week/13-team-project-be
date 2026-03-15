package com.matchimban.matchimban_api.ragchat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대화 이력 메시지 항목")
public record RagHistoryMessageItem(
	@Schema(example = "42")
	Long id,
	@Schema(example = "assistant")
	String role,
	@Schema(example = "주차 가능하고 6인 룸이 있는 곳으로 다시 찾아보면...")
	String content,
	@JsonProperty("created_at")
	@Schema(example = "2026-03-11 10:00:01")
	String createdAt
) {
}
