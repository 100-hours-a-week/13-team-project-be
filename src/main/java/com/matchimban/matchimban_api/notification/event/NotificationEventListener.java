package com.matchimban.matchimban_api.notification.event;

import com.matchimban.matchimban_api.notification.service.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCommandService notificationCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        try {
            notificationCommandService.createNotifications(event);
        } catch (Exception ex) {
            log.error("Failed to persist notification from eventKey={}", event.eventKey(), ex);
        }
    }
}
