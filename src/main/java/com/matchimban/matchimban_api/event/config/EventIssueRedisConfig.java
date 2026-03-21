package com.matchimban.matchimban_api.event.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class EventIssueRedisConfig {

    @Bean
    public RedisScript<List> eventIssueEnqueueScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/event-issue-enqueue.lua")));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public RedisScript<List> eventIssueClaimScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/event-issue-claim.lua")));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public RedisScript<Long> eventIssueRequeueExpiredProcessingScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/event-issue-requeue-expired-processing.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
