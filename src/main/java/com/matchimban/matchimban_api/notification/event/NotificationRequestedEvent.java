package com.matchimban.matchimban_api.notification.event;

import com.matchimban.matchimban_api.notification.entity.NotificationType;
import java.util.List;

public record NotificationRequestedEvent(
        NotificationType notificationType,
        String title,
        String content,
        String targetType,
        Long targetId,
        Long subTargetId,
        String deeplinkPath,
        String eventKey,
        String payloadJson,
        List<Long> recipientMemberIds
) {

    public NotificationRequestedEvent {
        recipientMemberIds = recipientMemberIds == null ? List.of() : List.copyOf(recipientMemberIds);
    }
}
