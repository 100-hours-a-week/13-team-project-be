package com.matchimban.matchimban_api.chat.dto.ws;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지 전송 ACK 이벤트")
public record ChatMessageSendAckEvent(
	@Schema(example = "message_send_ack")
	String event,
	ChatMessageSendAckData data
) {
	public static ChatMessageSendAckEvent accepted(ChatMessageSendAckData data) {
		return new ChatMessageSendAckEvent("message_send_ack", data);
	}
}
