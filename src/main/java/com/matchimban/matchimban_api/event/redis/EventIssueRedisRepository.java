package com.matchimban.matchimban_api.event.redis;

import com.matchimban.matchimban_api.event.config.EventIssueProperties;
import com.matchimban.matchimban_api.event.dto.response.EventIssueStatusResponse;
import com.matchimban.matchimban_api.event.entity.Event;
import com.matchimban.matchimban_api.event.entity.EventIssueFailureReason;
import com.matchimban.matchimban_api.event.entity.EventIssueRequestStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventIssueRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final EventIssueRedisKeyFactory keyFactory;
    private final EventIssueProperties properties;

    public void syncMeta(Event event, int queueLimit) {
        String key = keyFactory.metaKey(event.getId());
        Map<String, String> values = new HashMap<>();
        values.put("startAtEpochMs", String.valueOf(event.getStartAt().toEpochMilli()));
        values.put("endAtEpochMs", String.valueOf(event.getEndAt().toEpochMilli()));
        values.put("capacity", String.valueOf(event.getCapacity()));
        values.put("queueLimit", String.valueOf(queueLimit));
        redisTemplate.opsForHash().putAll(key, values);
    }

    public int getRetryCount(Long eventId, String requestId) {
        Object raw = redisTemplate.opsForHash().get(keyFactory.requestKey(eventId, requestId), "retryCount");
        if (raw == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    public boolean hasIssuedUsersCache(Long eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(keyFactory.issuedUsersKey(eventId)));
    }

    public void cacheIssuedUsers(Long eventId, List<Long> memberIds) {
        redisTemplate.opsForSet().add(keyFactory.issuedUsersKey(eventId), "__seed__");
        if (!memberIds.isEmpty()) {
            String[] values = memberIds.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(keyFactory.issuedUsersKey(eventId), values);
        }
    }

    public String getCurrentRequestId(Long eventId, Long memberId) {
        return redisTemplate.opsForValue().get(keyFactory.userRequestKey(eventId, memberId));
    }

    public Long getQueuePosition(Long eventId, String requestId) {
        Long rank = redisTemplate.opsForZSet().rank(keyFactory.queueKey(eventId), requestId);
        return rank == null ? null : rank + 1;
    }

    public Map<Object, Object> getRequest(Long eventId, String requestId) {
        return redisTemplate.opsForHash().entries(keyFactory.requestKey(eventId, requestId));
    }

    public void markSucceeded(
            Long eventId,
            Long memberId,
            String requestId,
            Long couponId,
            Instant issuedAt,
            Instant expiredAt
    ) {
        String requestKey = keyFactory.requestKey(eventId, requestId);
        Map<String, String> values = new HashMap<>();
        values.put("status", EventIssueRequestStatus.SUCCEEDED.name());
        values.put("reason", "");
        values.put("couponId", String.valueOf(couponId));
        values.put("issuedAt", issuedAt.toString());
        values.put("expiredAt", expiredAt.toString());
        values.put("updatedAt", Instant.now().toString());
        redisTemplate.opsForHash().putAll(requestKey, values);
        clearRuntimeQueueKeys(eventId, requestId);
        redisTemplate.opsForSet().add(keyFactory.issuedUsersKey(eventId), String.valueOf(memberId));
        applyTerminalTtl(eventId, memberId, requestId);
    }

    public void markFailed(
            Long eventId,
            Long memberId,
            String requestId,
            EventIssueFailureReason reason
    ) {
        String requestKey = keyFactory.requestKey(eventId, requestId);
        Map<String, String> values = new HashMap<>();
        values.put("status", EventIssueRequestStatus.FAILED.name());
        values.put("reason", reason.name());
        values.put("updatedAt", Instant.now().toString());
        redisTemplate.opsForHash().putAll(requestKey, values);
        clearRuntimeQueueKeys(eventId, requestId);
        applyTerminalTtl(eventId, memberId, requestId);
    }

    public void requeueForRetry(Long eventId, String requestId) {
        String requestKey = keyFactory.requestKey(eventId, requestId);
        String queueScoreRaw = asString(redisTemplate.opsForHash().get(requestKey, "queueScore"));
        double queueScore = queueScoreRaw == null || queueScoreRaw.isBlank()
                ? Instant.now().toEpochMilli()
                : Double.parseDouble(queueScoreRaw);

        redisTemplate.opsForHash().put(requestKey, "status", EventIssueRequestStatus.WAITING.name());
        redisTemplate.opsForHash().put(requestKey, "reason", "");
        redisTemplate.opsForHash().put(requestKey, "updatedAt", Instant.now().toString());
        redisTemplate.opsForHash().increment(requestKey, "retryCount", 1);
        redisTemplate.opsForZSet().remove(keyFactory.processingKey(eventId), requestId);
        redisTemplate.opsForZSet().add(keyFactory.queueKey(eventId), requestId, queueScore);
    }

    public EventIssueStatusResponse toStatusResponse(Long eventId, String requestId, Map<Object, Object> requestData) {
        String statusRaw = asString(requestData.get("status"));
        String reasonRaw = asString(requestData.get("reason"));
        String couponIdRaw = asString(requestData.get("couponId"));
        String issuedAtRaw = asString(requestData.get("issuedAt"));
        String expiredAtRaw = asString(requestData.get("expiredAt"));

        EventIssueRequestStatus status = EventIssueRequestStatus.valueOf(statusRaw);
        EventIssueFailureReason reason = reasonRaw == null || reasonRaw.isBlank()
                ? null
                : EventIssueFailureReason.valueOf(reasonRaw);

        Long queuePosition = status == EventIssueRequestStatus.WAITING ? getQueuePosition(eventId, requestId) : null;

        return new EventIssueStatusResponse(
                requestId,
                status,
                reason,
                queuePosition,
                couponIdRaw == null || couponIdRaw.isBlank() ? null : Long.parseLong(couponIdRaw),
                issuedAtRaw == null || issuedAtRaw.isBlank() ? null : Instant.parse(issuedAtRaw),
                expiredAtRaw == null || expiredAtRaw.isBlank() ? null : Instant.parse(expiredAtRaw)
        );
    }

    private void applyTerminalTtl(Long eventId, Long memberId, String requestId) {
        Duration ttl = properties.terminalStatusTtl();
        redisTemplate.expire(keyFactory.requestKey(eventId, requestId), ttl);
        redisTemplate.expire(keyFactory.userRequestKey(eventId, memberId), ttl);
    }

    private void clearRuntimeQueueKeys(Long eventId, String requestId) {
        redisTemplate.opsForZSet().remove(keyFactory.queueKey(eventId), requestId);
        redisTemplate.opsForZSet().remove(keyFactory.processingKey(eventId), requestId);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
