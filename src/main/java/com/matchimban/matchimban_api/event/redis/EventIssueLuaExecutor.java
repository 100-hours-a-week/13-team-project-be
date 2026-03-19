package com.matchimban.matchimban_api.event.redis;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventIssueLuaExecutor {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> eventIssueEnqueueScript;
    private final RedisScript<List> eventIssueClaimScript;
    private final RedisScript<Long> eventIssueRequeueExpiredProcessingScript;

    public List<?> enqueue(List<String> keys, List<String> args) {
        return redisTemplate.execute(eventIssueEnqueueScript, keys, args.toArray());
    }

    public List<?> claim(List<String> keys, List<String> args) {
        return redisTemplate.execute(eventIssueClaimScript, keys, args.toArray());
    }

    public Long requeueExpiredProcessing(List<String> keys, List<String> args) {
        return redisTemplate.execute(eventIssueRequeueExpiredProcessingScript, keys, args.toArray());
    }
}
