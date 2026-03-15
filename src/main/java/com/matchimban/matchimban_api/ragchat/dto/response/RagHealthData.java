package com.matchimban.matchimban_api.ragchat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RAG 서버 헬스 상태")
public record RagHealthData(
	@Schema(example = "ok")
	String status
) {
}
