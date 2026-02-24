package com.matchimban.matchimban_api.chat.dto.ws;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "새 메시지 브로드캐스트 이벤트")
public record ChatMessageCreatedEvent(
	@Schema(example = "message_created")
	String event,
	ChatMessageCreatedData data
) {
	public static ChatMessageCreatedEvent of(ChatMessageCreatedData data) {
		return new ChatMessageCreatedEvent("message_created", data);
	}
}
