package com.matchimban.matchimban_api.notification.service;

import com.matchimban.matchimban_api.notification.config.NotificationProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final NotificationProperties notificationProperties;
    private final NotificationOutboxService notificationOutboxService;
    private final NotificationDispatchTxService notificationDispatchTxService;

    private final String workerId = "noti-dispatch-" + UUID.randomUUID();

    public void tick() {
        if (!notificationProperties.getDispatch().isEnabled()) {
            return;
        }

        notificationOutboxService.recoverStaleLocks(notificationProperties.getDispatch().getStaleLockThreshold());

        int batchSize = Math.max(1, notificationProperties.getDispatch().getBatchSize());
        for (int i = 0; i < batchSize; i++) {
            Long outboxId = notificationOutboxService.claimNext(workerId);
            if (outboxId == null) {
                return;
            }

            try {
                notificationDispatchTxService.processClaimedOutbox(outboxId);
            } catch (Exception ex) {
                log.error("Failed to process notification outbox. outboxId={}", outboxId, ex);
            }
        }
    }
}
