package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지별 unread_count 항목")
public record ChatUnreadCountItem(
	@JsonProperty("message_id")
	@Schema(example = "6650a1b2c3d4e5f678901234")
	String messageId,
	@JsonProperty("unread_count")
	@Schema(example = "2")
	int unreadCount
) {
}
