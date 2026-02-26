package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "메시지 전송 요청")
public record ChatSendMessageRequest(
	@JsonProperty("client_message_id")
	@Schema(
		description = "클라이언트 임시 메시지 식별자",
		example = "8c2f3b1a-8d4d-4dd8-9a10-4d8b0d9f8f21"
	)
	@Size(max = 64)
	String clientMessageId,
	@NotNull
	@Schema(example = "TEXT")
	ChatMessageType type,
	@NotBlank
	@Size(max = 5000)
	@Schema(example = "저도 투표했습니다!")
	String content
) {
}
