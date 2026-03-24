package com.matchimban.matchimban_api.settlement.redis;

import com.matchimban.matchimban_api.settlement.service.SettlementProgressSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementProgressRedisSubscriber implements MessageListener {

    private final SettlementProgressSseService settlementProgressSseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            Long meetingId = Long.parseLong(payload);
            settlementProgressSseService.publish(meetingId);
        } catch (NumberFormatException e) {
            log.warn("Ignore invalid settlement progress payload={}", payload);
        } catch (Exception e) {
            log.error("Failed to publish settlement progress payload={}", payload, e);
        }
    }
}
