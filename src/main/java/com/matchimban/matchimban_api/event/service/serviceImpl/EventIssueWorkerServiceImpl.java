package com.matchimban.matchimban_api.event.service.serviceImpl;

import com.matchimban.matchimban_api.event.config.EventIssueProperties;
import com.matchimban.matchimban_api.event.entity.EventIssueFailureReason;
import com.matchimban.matchimban_api.event.redis.EventIssueLuaExecutor;
import com.matchimban.matchimban_api.event.redis.EventIssueRedisKeyFactory;
import com.matchimban.matchimban_api.event.redis.EventIssueRedisRepository;
import com.matchimban.matchimban_api.event.repository.EventRepository;
import com.matchimban.matchimban_api.event.service.EventIssueFinalizeService;
import com.matchimban.matchimban_api.event.service.EventIssueWorkerService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventIssueWorkerServiceImpl implements EventIssueWorkerService {

    private final EventRepository eventRepository;
    private final EventIssueProperties properties;
    private final EventIssueLuaExecutor luaExecutor;
    private final EventIssueRedisKeyFactory keyFactory;
    private final EventIssueRedisRepository eventIssueRedisRepository;
    private final EventIssueFinalizeService eventIssueFinalizeService;

    @Override
    public void processBatchForActiveEvents() {
        Instant now = Instant.now();
        List<Long> eventIds = eventRepository.findIssueTargetEventIds(now);
        for (Long eventId : eventIds) {
            requeueExpiredProcessing(eventId, now);
            for (int i = 0; i < properties.batchSizePerEvent(); i++) {
                List<?> claimResult = luaExecutor.claim(
                        List.of(
                                keyFactory.queueKey(eventId),
                                keyFactory.processingKey(eventId),
                                keyFactory.requestKey(eventId, "")
                        ),
                        List.of(
                                String.valueOf(now.toEpochMilli()),
                                String.valueOf(now.plus(properties.processingLease()).toEpochMilli())
                        )
                );
                if (claimResult == null || claimResult.isEmpty()) {
                    break;
                }

                String requestId = String.valueOf(claimResult.get(0));
                Long memberId = Long.parseLong(String.valueOf(claimResult.get(1)));
                processClaimedRequest(eventId, requestId, memberId);
            }
        }
    }

    private void processClaimedRequest(Long eventId, String requestId, Long memberId) {
        try {
            EventIssueFinalizeService.FinalizeResult result = eventIssueFinalizeService.finalizeIssue(eventId, memberId);
            if (result.success()) {
                eventIssueRedisRepository.markSucceeded(
                        eventId,
                        memberId,
                        requestId,
                        result.couponId(),
                        result.issuedAt(),
                        result.expiredAt()
                );
                return;
            }
            eventIssueRedisRepository.markFailed(
                    eventId,
                    memberId,
                    requestId,
                    result.failureReason()
            );
        } catch (Exception ex) {
            log.error("Failed to finalize event issue request. eventId={}, requestId={}, memberId={}",
                    eventId, requestId, memberId, ex);
            int retryCount = eventIssueRedisRepository.getRetryCount(eventId, requestId);
            if (retryCount < properties.maxFinalizeRetries()) {
                eventIssueRedisRepository.requeueForRetry(eventId, requestId);
                return;
            }
            eventIssueRedisRepository.markFailed(eventId, memberId, requestId, EventIssueFailureReason.SYSTEM_ERROR);
        }
    }

    private void requeueExpiredProcessing(Long eventId, Instant now) {
        luaExecutor.requeueExpiredProcessing(
                List.of(
                        keyFactory.processingKey(eventId),
                        keyFactory.queueKey(eventId),
                        keyFactory.requestKey(eventId, "")
                ),
                List.of(
                        String.valueOf(now.toEpochMilli()),
                        String.valueOf(properties.batchSizePerEvent())
                )
        );
    }
}
