package com.matchimban.matchimban_api.chat.config;

import com.matchimban.matchimban_api.chat.redis.ChatRedisSubscriber;
import com.matchimban.matchimban_api.chat.redis.ChatUnreadCountsRedisSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = "chat.redis.listener.enabled", havingValue = "true", matchIfMissing = true)
public class ChatRedisPubSubConfig {

	@Value("${chat.redis.channel.message-created:chat:meeting:message-created}")
	private String messageCreatedChannel;

	@Value("${chat.redis.channel.unread-counts-updated:chat:meeting:unread-counts-updated}")
	private String unreadCountsUpdatedChannel;

	@Bean
	public RedisMessageListenerContainer chatRedisMessageListenerContainer(
			RedisConnectionFactory redisConnectionFactory,
			ChatRedisSubscriber chatRedisSubscriber,
			ChatUnreadCountsRedisSubscriber chatUnreadCountsRedisSubscriber
		) {
			RedisMessageListenerContainer container = new RedisMessageListenerContainer();
			container.setConnectionFactory(redisConnectionFactory);
			container.addMessageListener(chatRedisSubscriber, new ChannelTopic(messageCreatedChannel));
			container.addMessageListener(chatUnreadCountsRedisSubscriber, new ChannelTopic(unreadCountsUpdatedChannel));
			return container;
		}
}
