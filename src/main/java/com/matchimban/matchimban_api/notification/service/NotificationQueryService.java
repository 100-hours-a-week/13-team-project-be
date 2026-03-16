package com.matchimban.matchimban_api.notification.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.notification.dto.response.NotificationItem;
import com.matchimban.matchimban_api.notification.dto.response.NotificationListResponse;
import com.matchimban.matchimban_api.notification.entity.Notification;
import com.matchimban.matchimban_api.notification.error.NotificationErrorCode;
import com.matchimban.matchimban_api.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private static final Instant INITIAL_CURSOR_CREATED_AT = Instant.parse("9999-12-31T23:59:59Z");
    private static final long INITIAL_CURSOR_ID = Long.MAX_VALUE;

    private final NotificationRepository notificationRepository;

    public NotificationListResponse getNotifications(Long memberId, Instant cursorCreatedAt, Long cursorId, int size) {
        validateCursor(cursorCreatedAt, cursorId);

        Pageable pageable = PageRequest.of(0, size + 1);
        Instant effectiveCursorCreatedAt = cursorCreatedAt == null ? INITIAL_CURSOR_CREATED_AT : cursorCreatedAt;
        Long effectiveCursorId = cursorId == null ? INITIAL_CURSOR_ID : cursorId;

        List<Notification> rows = notificationRepository.findFeedPage(
                memberId,
                effectiveCursorCreatedAt,
                effectiveCursorId,
                pageable
        );

        boolean hasNext = rows.size() > size;
        List<Notification> pageRows = hasNext ? rows.subList(0, size) : rows;

        Instant nextCursorCreatedAt = null;
        Long nextCursorId = null;
        if (hasNext && !pageRows.isEmpty()) {
            Notification last = pageRows.get(pageRows.size() - 1);
            nextCursorCreatedAt = last.getCreatedAt();
            nextCursorId = last.getId();
        }

        List<NotificationItem> items = pageRows.stream()
                .map(this::toItem)
                .toList();

        long unreadCount = notificationRepository.countByMemberIdAndIsDeletedFalseAndReadAtIsNull(memberId);

        return new NotificationListResponse(items, nextCursorCreatedAt, nextCursorId, hasNext, unreadCount);
    }

    private NotificationItem toItem(Notification notification) {
        return new NotificationItem(
                notification.getId(),
                notification.getNotiType().name(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getSubTargetId(),
                notification.getDeeplinkPath(),
                notification.getPayloadJson(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getReadAt() != null
        );
    }

    private void validateCursor(Instant cursorCreatedAt, Long cursorId) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new ApiException(NotificationErrorCode.INVALID_CURSOR);
        }
    }
}
