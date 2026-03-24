package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.settlement.dto.response.SettlementProgressResponse;
import com.matchimban.matchimban_api.settlement.redis.SettlementProgressRedisPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SettlementProgressSseService {

    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;
    private static final String EVENT_NAME = "settlement-progress";

    private final SettlementProgressService settlementProgressService;
    private final SettlementProgressRedisPublisher settlementProgressRedisPublisher;

    private final Map<Long, Set<Subscriber>> subscribersByMeetingId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long meetingId, Long memberId) {
        SettlementProgressResponse initialResponse = settlementProgressService.getProgress(meetingId, memberId);

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        Subscriber subscriber = new Subscriber(memberId, emitter);

        subscribersByMeetingId
                .computeIfAbsent(meetingId, ignored -> ConcurrentHashMap.newKeySet())
                .add(subscriber);

        emitter.onCompletion(() -> removeSubscriber(meetingId, subscriber));
        emitter.onTimeout(() -> removeSubscriber(meetingId, subscriber));
        emitter.onError(ignored -> removeSubscriber(meetingId, subscriber));

        send(emitter, initialResponse);
        return emitter;
    }

    public void publish(Long meetingId) {
        Set<Subscriber> subscribers = subscribersByMeetingId.get(meetingId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (Subscriber subscriber : subscribers.toArray(new Subscriber[0])) {
            try {
                SettlementProgressResponse response =
                        settlementProgressService.getProgress(meetingId, subscriber.memberId());
                send(subscriber.emitter(), response);
            } catch (Exception e) {
                removeSubscriber(meetingId, subscriber);
                subscriber.emitter().complete();
            }
        }
    }

    public void publishAfterCommit(Long meetingId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            settlementProgressRedisPublisher.publish(meetingId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                settlementProgressRedisPublisher.publish(meetingId);
            }
        });
    }

    private void send(SseEmitter emitter, SettlementProgressResponse response) {
        try {
            emitter.send(SseEmitter.event().name(EVENT_NAME).data(response));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void removeSubscriber(Long meetingId, Subscriber subscriber) {
        Set<Subscriber> subscribers = subscribersByMeetingId.get(meetingId);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(subscriber);
        if (subscribers.isEmpty()) {
            subscribersByMeetingId.remove(meetingId);
        }
    }

    private record Subscriber(Long memberId, SseEmitter emitter) {}
}
