package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.matchimban.matchimban_api.chat.dto.ChatSenderDto;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "새 메시지 이벤트 본문")
public record ChatMessageCreatedData(
	@JsonProperty("meeting_id")
	@Schema(example = "10")
	Long meetingId,
	@JsonProperty("message_id")
	@Schema(example = "13003")
	Long messageId,
	@Schema(example = "TEXT")
	ChatMessageType type,
	@Schema(example = "저도 투표했습니다!")
	String content,
	ChatSenderDto sender,
	@JsonProperty("created_at")
	@Schema(example = "2026-01-08T15:11:02+09:00")
	OffsetDateTime createdAt
) {
}
