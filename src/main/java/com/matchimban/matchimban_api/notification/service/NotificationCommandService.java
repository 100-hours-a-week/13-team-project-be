package com.matchimban.matchimban_api.notification.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.notification.entity.Notification;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.error.NotificationErrorCode;
import com.matchimban.matchimban_api.notification.event.NotificationRequestedEvent;
import com.matchimban.matchimban_api.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotifications(NotificationRequestedEvent event) {
        if (event == null || event.recipientMemberIds().isEmpty()) {
            return;
        }

        Set<Long> recipients = new LinkedHashSet<>(event.recipientMemberIds());
        for (Long recipientMemberId : recipients) {
            if (recipientMemberId == null) {
                continue;
            }
            try {
                createSingleNotification(event, recipientMemberId);
            } catch (DataIntegrityViolationException ex) {
                // UNIQUE(member_id, event_key) 충돌이면 이미 생성된 알림으로 간주한다.
                log.debug("Duplicate notification skipped. memberId={}, eventKey={}", recipientMemberId, event.eventKey());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotifications(
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
        createNotifications(
                new NotificationRequestedEvent(
                        notificationType,
                        title,
                        content,
                        targetType,
                        targetId,
                        subTargetId,
                        deeplinkPath,
                        eventKey,
                        payloadJson,
                        recipientMemberIds
                )
        );
    }

    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndMemberIdAndIsDeletedFalse(notificationId, memberId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markReadIfUnread(Instant.now());
    }

    @Transactional
    public int markAllRead(Long memberId) {
        return notificationRepository.markAllRead(memberId, Instant.now());
    }

    @Transactional
    public void softDelete(Long memberId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndMemberIdAndIsDeletedFalse(notificationId, memberId)
                .orElseThrow(() -> new ApiException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.softDelete(Instant.now());
    }

    private void createSingleNotification(NotificationRequestedEvent event, Long recipientMemberId) {
        Member memberRef = entityManager.getReference(Member.class, recipientMemberId);
        Notification notification = Notification.builder()
                .member(memberRef)
                .notiType(event.notificationType())
                .title(trimWithDefault(event.title(), 50, "알림"))
                .content(trimWithDefault(event.content(), 5000, "새 알림이 도착했습니다."))
                .targetType(trimWithDefault(event.targetType(), 30, "UNKNOWN"))
                .targetId(event.targetId())
                .subTargetId(event.subTargetId())
                .deeplinkPath(trimNullable(event.deeplinkPath(), 255))
                .payloadJson(trimNullable(event.payloadJson(), 4000))
                .eventKey(trimWithDefault(event.eventKey(), 120, buildFallbackEventKey(event, recipientMemberId)))
                .build();

        Notification saved = notificationRepository.saveAndFlush(notification);
        notificationOutboxService.createOutbox(saved);
    }

    private String buildFallbackEventKey(NotificationRequestedEvent event, Long memberId) {
        String type = event.notificationType() == null ? "UNKNOWN" : event.notificationType().name();
        return type + ":" + memberId + ":" + Instant.now().toEpochMilli();
    }

    private String trimNullable(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String trimWithDefault(String value, int maxLength, String fallback) {
        String normalized = trimNullable(value, maxLength);
        if (!StringUtils.hasText(normalized)) {
            return fallback;
        }
        return normalized;
    }
}
