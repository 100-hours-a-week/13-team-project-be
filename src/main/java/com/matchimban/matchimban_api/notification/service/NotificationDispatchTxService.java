package com.matchimban.matchimban_api.notification.service;

import com.matchimban.matchimban_api.notification.config.NotificationProperties;
import com.matchimban.matchimban_api.notification.entity.NotificationOutbox;
import com.matchimban.matchimban_api.notification.entity.NotificationOutboxStatus;
import com.matchimban.matchimban_api.notification.entity.NotificationToken;
import com.matchimban.matchimban_api.notification.fcm.FcmMessageSender;
import com.matchimban.matchimban_api.notification.repository.NotificationOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchTxService {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final FcmMessageSender fcmMessageSender;
    private final NotificationProperties notificationProperties;

    @Transactional
    public void processClaimedOutbox(Long outboxId) {
        NotificationOutbox outbox = notificationOutboxRepository.findById(outboxId).orElse(null);
        if (outbox == null || outbox.getStatus() != NotificationOutboxStatus.IN_PROGRESS) {
            return;
        }

        Instant now = Instant.now();
        String tokenSnapshot = resolveTokenSnapshot(outbox);
        if (!StringUtils.hasText(tokenSnapshot)) {
            outbox.markDead("TOKEN_MISSING", "active token not found", now);
            return;
        }

        FcmMessageSender.SendResult sendResult = fcmMessageSender.send(outbox.getNotification(), tokenSnapshot);
        if (sendResult.success()) {
            outbox.markSent(now);
            return;
        }

        if (sendResult.permanentFailure()) {
            outbox.markDead(sendResult.errorCode(), sendResult.errorMessage(), now);
            deactivateTokenIfPermanent(outbox, sendResult.errorCode(), now);
            return;
        }

        if (outbox.hasReachedAttemptLimit(notificationProperties.getRetry().getMaxAttempts())) {
            outbox.markDead(sendResult.errorCode(), sendResult.errorMessage(), now);
            return;
        }

        Duration delay = NotificationRetryPolicy.backoffForAttempt(outbox.getAttemptCount());
        if (delay == null) {
            outbox.markDead(sendResult.errorCode(), sendResult.errorMessage(), now);
            return;
        }

        outbox.markFailed(sendResult.errorCode(), sendResult.errorMessage(), now.plus(delay));
    }

    private String resolveTokenSnapshot(NotificationOutbox outbox) {
        NotificationToken token = outbox.getNotificationToken();
        if (token == null) {
            return outbox.getTokenSnapshot();
        }

        if (!token.isActive()) {
            return null;
        }
        if (token.getMember() == null || !token.getMember().getId().equals(outbox.getMember().getId())) {
            return null;
        }

        return token.getFcmToken();
    }

    private void deactivateTokenIfPermanent(NotificationOutbox outbox, String errorCode, Instant now) {
        if (!isTokenDeactivateCode(errorCode)) {
            return;
        }

        NotificationToken token = outbox.getNotificationToken();
        if (token == null || !token.isActive()) {
            return;
        }

        token.deactivate(now);
        log.info("Deactivate notification token due to permanent FCM failure. tokenId={}, code={}",
                token.getId(), errorCode);
    }

    private boolean isTokenDeactivateCode(String errorCode) {
        if (!StringUtils.hasText(errorCode)) {
            return false;
        }
        return "UNREGISTERED".equalsIgnoreCase(errorCode)
                || "INVALID_ARGUMENT".equalsIgnoreCase(errorCode);
    }
}
