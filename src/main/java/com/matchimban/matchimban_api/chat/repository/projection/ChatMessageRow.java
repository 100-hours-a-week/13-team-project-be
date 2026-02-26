package com.matchimban.matchimban_api.chat.repository.projection;

import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import java.time.Instant;

public record ChatMessageRow(
	Long messageId,
	ChatMessageType type,
	String content,
	Instant createdAt,
	Long senderId,
	String senderName,
	String senderProfileImageUrl
) {
}
