package com.matchimban.matchimban_api.chat.event;

import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageCreatedEvent;

public record ChatMessageCreatedInternalEvent(
	ChatMessageCreatedEvent payload
) {
}
