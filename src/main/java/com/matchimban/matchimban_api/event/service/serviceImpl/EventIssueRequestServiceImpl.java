package com.matchimban.matchimban_api.event.service.serviceImpl;

import com.matchimban.matchimban_api.event.config.EventIssueProperties;
import com.matchimban.matchimban_api.event.dto.response.EventIssueRequestResponse;
import com.matchimban.matchimban_api.event.dto.response.EventIssueStatusResponse;
import com.matchimban.matchimban_api.event.entity.Event;
import com.matchimban.matchimban_api.event.entity.EventIssueFailureReason;
import com.matchimban.matchimban_api.event.entity.EventIssueRequestStatus;
import com.matchimban.matchimban_api.event.error.EventErrorCode;
import com.matchimban.matchimban_api.event.redis.EventIssueLuaExecutor;
import com.matchimban.matchimban_api.event.redis.EventIssueRedisKeyFactory;
import com.matchimban.matchimban_api.event.redis.EventIssueRedisRepository;
import com.matchimban.matchimban_api.event.repository.EventCouponRepository;
import com.matchimban.matchimban_api.event.repository.EventRepository;
import com.matchimban.matchimban_api.event.service.EventIssueRequestService;
import com.matchimban.matchimban_api.event.service.EventIssueStatusService;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventIssueRequestServiceImpl implements EventIssueRequestService {

    private final EventRepository eventRepository;
    private final EventCouponRepository eventCouponRepository;
    private final EventIssueRedisRepository eventIssueRedisRepository;
    private final EventIssueRedisKeyFactory keyFactory;
    private final EventIssueLuaExecutor luaExecutor;
    private final EventIssueProperties properties;
    private final EventIssueStatusService eventIssueStatusService;

    @Override
    public EventIssueRequestResponse submit(Long memberId, Long eventId) {
        Event event = eventRepository.findByIdAndIsActiveTrueAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ApiException(EventErrorCode.EVENT_NOT_FOUND));

        int queueLimit = calculateQueueLimit(event.getCapacity());
        eventIssueRedisRepository.syncMeta(event, queueLimit);
        ensureIssuedUsersCache(eventId);

        String requestId = UUID.randomUUID().toString();
        List<?> result = luaExecutor.enqueue(
                List.of(
                        keyFactory.metaKey(eventId),
                        keyFactory.issuedUsersKey(eventId),
                        keyFactory.userRequestKey(eventId, memberId),
                        keyFactory.queueKey(eventId),
                        keyFactory.requestKey(eventId, requestId)
                ),
                List.of(
                        String.valueOf(memberId),
                        requestId,
                        String.valueOf(Instant.now().toEpochMilli()),
                        String.valueOf(queueLimit),
                        String.valueOf(properties.terminalStatusTtl().toSeconds())
                )
        );

        long resultCode = asLong(result, 0);
        String resultRequestId = asString(result, 1);
        Long queuePosition = asNullableLong(result, 2);

        if (resultCode == 0L) {
            return new EventIssueRequestResponse(
                    resultRequestId,
                    EventIssueRequestStatus.WAITING,
                    null,
                    queuePosition,
                    properties.pollingIntervalMillis()
            );
        }
        if (resultCode == 3L) {
            EventIssueStatusResponse current = eventIssueStatusService.getCurrentStatus(memberId, eventId);
            return new EventIssueRequestResponse(
                    current.requestId(),
                    current.status(),
                    EventIssueFailureReason.ALREADY_WAITING,
                    current.queuePosition(),
                    properties.pollingIntervalMillis()
            );
        }

        return new EventIssueRequestResponse(
                resultRequestId == null || resultRequestId.isBlank() ? null : resultRequestId,
                mapStatus(resultCode),
                mapReason(resultCode),
                queuePosition,
                properties.pollingIntervalMillis()
        );
    }

    private void ensureIssuedUsersCache(Long eventId) {
        if (eventIssueRedisRepository.hasIssuedUsersCache(eventId)) {
            return;
        }
        eventIssueRedisRepository.cacheIssuedUsers(eventId, eventCouponRepository.findIssuedMemberIdsByEventId(eventId));
    }

    private int calculateQueueLimit(int capacity) {
        return Math.min((int) Math.floor(capacity * 1.1), capacity + 300);
    }

    private EventIssueRequestStatus mapStatus(long resultCode) {
        return resultCode == 0L ? EventIssueRequestStatus.WAITING : EventIssueRequestStatus.FAILED;
    }

    private EventIssueFailureReason mapReason(long resultCode) {
        return switch ((int) resultCode) {
            case 1 -> EventIssueFailureReason.EVENT_NOT_STARTED;
            case 2 -> EventIssueFailureReason.ALREADY_ISSUED;
            case 3 -> EventIssueFailureReason.ALREADY_WAITING;
            case 4 -> EventIssueFailureReason.QUEUE_LIMIT_EXCEEDED;
            case 5 -> EventIssueFailureReason.EVENT_ENDED;
            default -> EventIssueFailureReason.SYSTEM_ERROR;
        };
    }

    private long asLong(List<?> values, int index) {
        return Long.parseLong(String.valueOf(values.get(index)));
    }

    private Long asNullableLong(List<?> values, int index) {
        String value = asString(values, index);
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private String asString(List<?> values, int index) {
        if (values == null || values.size() <= index || values.get(index) == null) {
            return null;
        }
        return String.valueOf(values.get(index));
    }
}
