package com.matchimban.matchimban_api.chat.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageCreatedEvent;
import com.matchimban.matchimban_api.chat.dto.ws.ChatUnreadCountsUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${chat.redis.channel.message-created:chat:meeting:message-created}")
	private String messageCreatedChannel;

	@Value("${chat.redis.channel.unread-counts-updated:chat:meeting:unread-counts-updated}")
	private String unreadCountsUpdatedChannel;

	public void publishMessageCreated(ChatMessageCreatedEvent payload) {
		publish(messageCreatedChannel, payload, "failed_to_publish_chat_message");
	}

	public void publishUnreadCountsUpdated(ChatUnreadCountsUpdatedEvent payload) {
		publish(unreadCountsUpdatedChannel, payload, "failed_to_publish_chat_unread_counts");
	}

	private void publish(String channel, Object payload, String errorCode) {
		try {
			String message = objectMapper.writeValueAsString(payload);
			redisTemplate.convertAndSend(channel, message);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException(errorCode, ex);
		}
	}
}
