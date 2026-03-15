package com.matchimban.matchimban_api.ragchat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RAG 챗봇 답변 데이터")
public record RagChatAskData(
	@Schema(example = "판교역 근처 주차 가능한 고기집으로 서현궁 백현점을 추천드려요...")
	String answer,
	@JsonProperty("user_id")
	@Schema(example = "member:123")
	String userId
) {
}
