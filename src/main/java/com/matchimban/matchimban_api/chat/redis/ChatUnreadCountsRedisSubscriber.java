package com.matchimban.matchimban_api.chat.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.chat.dto.ws.ChatUnreadCountsUpdatedEvent;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatUnreadCountsRedisSubscriber implements MessageListener {

	private final ObjectMapper objectMapper;
	private final SimpMessagingTemplate simpMessagingTemplate;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String raw = new String(message.getBody(), StandardCharsets.UTF_8);
		try {
			ChatUnreadCountsUpdatedEvent event = objectMapper.readValue(raw, ChatUnreadCountsUpdatedEvent.class);
			Long meetingId = event.data().meetingId();
			simpMessagingTemplate.convertAndSend(
				"/api/v2/topic/meetings/" + meetingId + "/unread-counts",
				event
			);
		} catch (Exception ex) {
			log.error("Failed to handle redis unread-counts message. payload={}", raw, ex);
		}
	}
}
