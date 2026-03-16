package com.matchimban.matchimban_api.ragchat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "RAG 대화 이력 조회 결과")
public record RagHistoryData(
	List<RagHistoryMessageItem> messages,
	@JsonProperty("next_cursor")
	@Schema(nullable = true, example = "41")
	Long nextCursor
) {
}
