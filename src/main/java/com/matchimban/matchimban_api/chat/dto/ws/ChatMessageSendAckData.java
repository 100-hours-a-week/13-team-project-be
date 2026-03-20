package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "메시지 전송 ACK 본문")
public record ChatMessageSendAckData(
	@JsonProperty("meeting_id")
	@Schema(example = "10")
	Long meetingId,
	@JsonProperty("client_message_id")
	@Schema(example = "8c2f3b1a-8d4d-4dd8-9a10-4d8b0d9f8f21")
	String clientMessageId,
	@JsonProperty("message_id")
	@Schema(example = "6650a1b2c3d4e5f678901234")
	String messageId,
	@Schema(example = "ACCEPTED")
	String status,
	@JsonProperty("created_at")
	@Schema(example = "2026-01-08T15:11:02+09:00")
	OffsetDateTime createdAt
) {
}
