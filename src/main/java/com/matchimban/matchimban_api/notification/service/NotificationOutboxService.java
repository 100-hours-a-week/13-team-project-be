package com.matchimban.matchimban_api.notification.service;

import com.matchimban.matchimban_api.notification.entity.Notification;
import com.matchimban.matchimban_api.notification.entity.NotificationOutbox;
import com.matchimban.matchimban_api.notification.entity.NotificationOutboxStatus;
import com.matchimban.matchimban_api.notification.entity.NotificationToken;
import com.matchimban.matchimban_api.notification.repository.NotificationOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationTokenService notificationTokenService;

    @Transactional
    public NotificationOutbox createOutbox(Notification notification) {
        Long memberId = notification.getMember().getId();
        NotificationToken token = notificationTokenService.findLatestActiveToken(memberId);

        NotificationOutbox outbox = NotificationOutbox.builder()
                .notification(notification)
                .member(notification.getMember())
                .notificationToken(token)
                .tokenSnapshot(token == null ? null : token.getFcmToken())
                .status(NotificationOutboxStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();

        return notificationOutboxRepository.save(outbox);
    }

    @Transactional
    public int recoverStaleLocks(Duration staleThreshold) {
        Instant staleBefore = Instant.now().minus(staleThreshold);
        return notificationOutboxRepository.recoverStaleLocks(staleBefore);
    }

    @Transactional
    public Long claimNext(String workerId) {
        return notificationOutboxRepository.findNextClaimableForUpdate()
                .map(outbox -> {
                    outbox.claim(workerId, Instant.now());
                    notificationOutboxRepository.save(outbox);
                    return outbox.getId();
                })
                .orElse(null);
    }

    @Transactional
    public int cleanupSentAndDead(Duration retention) {
        Instant cutoff = Instant.now().minus(retention);
        return notificationOutboxRepository.cleanupSentAndDead(cutoff);
    }
}
