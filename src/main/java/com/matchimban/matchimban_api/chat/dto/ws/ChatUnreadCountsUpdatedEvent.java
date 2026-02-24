package com.matchimban.matchimban_api.chat.dto.ws;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "unread_count 브로드캐스트 이벤트")
public record ChatUnreadCountsUpdatedEvent(
	@Schema(example = "unread_counts_updated")
	String event,
	ChatUnreadCountsUpdatedData data
) {
	public static ChatUnreadCountsUpdatedEvent of(ChatUnreadCountsUpdatedData data) {
		return new ChatUnreadCountsUpdatedEvent("unread_counts_updated", data);
	}
}
