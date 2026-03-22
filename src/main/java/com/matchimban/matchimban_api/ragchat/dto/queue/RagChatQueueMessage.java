package com.matchimban.matchimban_api.ragchat.dto.queue;

public record RagChatQueueMessage(
	String requestId,
	String userId,
	String question
) {
}
