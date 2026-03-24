package com.matchimban.matchimban_api.settlement.config;

import com.matchimban.matchimban_api.settlement.redis.SettlementProgressRedisSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = "settlement.redis.listener.enabled", havingValue = "true", matchIfMissing = true)
public class SettlementRedisPubSubConfig {

    @Value("${settlement.redis.channel.progress-updated:settlement:meeting:progress-updated}")
    private String settlementProgressChannel;

    @Bean
    public RedisMessageListenerContainer settlementRedisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            SettlementProgressRedisSubscriber settlementProgressRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(settlementProgressRedisSubscriber, new ChannelTopic(settlementProgressChannel));
        return container;
    }
}
