package com.matchimban.matchimban_api.event.redis;

import org.springframework.stereotype.Component;

@Component
public class EventIssueRedisKeyFactory {

    public String metaKey(Long eventId) {
        return "event:" + eventId + ":issue:meta";
    }

    public String queueKey(Long eventId) {
        return "event:" + eventId + ":issue:queue";
    }

    public String processingKey(Long eventId) {
        return "event:" + eventId + ":issue:processing";
    }

    public String requestKey(Long eventId, String requestId) {
        return "event:" + eventId + ":issue:req:" + requestId;
    }

    public String userRequestKey(Long eventId, Long memberId) {
        return "event:" + eventId + ":issue:user:" + memberId;
    }

    public String issuedUsersKey(Long eventId) {
        return "event:" + eventId + ":issue:issued-users";
    }
}
