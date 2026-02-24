package com.matchimban.matchimban_api.chat.dto.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.matchimban.matchimban_api.chat.dto.ChatSenderDto;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "메시지 항목")
public record ChatMessageItemDto(
	@JsonProperty("message_id")
	@Schema(example = "13001")
	Long messageId,
	@JsonProperty("unread_count")
	@JsonInclude(JsonInclude.Include.NON_NULL)
		@Schema(
			description = "현재 시점 기준 message_id 단위 미열람자 수 (읽음 정책/윈도우 기반)",
			nullable = true,
			example = "2"
		)
		Integer unreadCount,
	@Schema(example = "TEXT")
	ChatMessageType type,
	@Schema(example = "안녕하세요! 내일 저녁 회식 장소 투표 부탁드립니다~")
	String content,
	@Schema(description = "SYSTEM 메시지면 null", nullable = true)
	ChatSenderDto sender,
	@JsonProperty("created_at")
	@Schema(example = "2026-01-08T15:11:02+09:00")
	OffsetDateTime createdAt
) {
}
