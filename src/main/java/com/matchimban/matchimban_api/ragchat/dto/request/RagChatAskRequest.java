package com.matchimban.matchimban_api.ragchat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "RAG 챗봇 질문 요청")
public record RagChatAskRequest(
	@JsonProperty("user_id")
	@NotBlank
	@Size(max = 100)
	@Schema(example = "member:123")
	String userId,
	@NotBlank
	@Size(max = 2000)
	@Schema(example = "판교 주차 가능한 고기집 추천해줘")
	String message
) {
}
