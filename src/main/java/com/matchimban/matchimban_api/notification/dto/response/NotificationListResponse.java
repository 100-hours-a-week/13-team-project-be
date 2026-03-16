package com.matchimban.matchimban_api.notification.dto.response;

import java.time.Instant;
import java.util.List;

public record NotificationListResponse(
        List<NotificationItem> items,
        Instant nextCursorCreatedAt,
        Long nextCursorId,
        boolean hasNext,
        long unreadCount
) {
}
