package com.matchimban.matchimban_api.settlement.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementProgressRedisPublisher {

    private final StringRedisTemplate redisTemplate;

    @Value("${settlement.redis.channel.progress-updated:settlement:meeting:progress-updated}")
    private String settlementProgressChannel;

    public void publish(Long meetingId) {
        redisTemplate.convertAndSend(settlementProgressChannel, String.valueOf(meetingId));
    }
}
