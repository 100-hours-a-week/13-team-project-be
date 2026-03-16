package com.matchimban.matchimban_api.notification.dto.response;

import java.time.Instant;

public record NotificationItem(
        Long id,
        String notiType,
        String title,
        String content,
        String targetType,
        Long targetId,
        Long subTargetId,
        String deeplinkPath,
        String payloadJson,
        Instant readAt,
        Instant createdAt,
        boolean read
) {
}
