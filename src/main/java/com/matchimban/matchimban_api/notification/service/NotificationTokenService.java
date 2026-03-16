package com.matchimban.matchimban_api.notification.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.notification.dto.request.NotificationTokenDeactivateRequest;
import com.matchimban.matchimban_api.notification.dto.request.NotificationTokenUpsertRequest;
import com.matchimban.matchimban_api.notification.entity.NotificationToken;
import com.matchimban.matchimban_api.notification.error.NotificationErrorCode;
import com.matchimban.matchimban_api.notification.repository.NotificationTokenRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NotificationTokenService {

    private final NotificationTokenRepository notificationTokenRepository;
    private final EntityManager entityManager;

    @Transactional
    public void upsertToken(Long memberId, NotificationTokenUpsertRequest request) {
        String fcmToken = normalizeRequired(request.fcmToken());
        String deviceKey = normalizeNullable(request.deviceKey(), 120);
        String userAgent = normalizeNullable(request.userAgent(), 500);

        Instant now = Instant.now();
        Member owner = entityManager.getReference(Member.class, memberId);

        deactivateDuplicatedDeviceTokens(memberId, deviceKey, fcmToken, now);

        notificationTokenRepository.findByFcmToken(fcmToken)
                .ifPresentOrElse(
                        existing -> existing.activate(owner, deviceKey, userAgent, now),
                        () -> notificationTokenRepository.save(
                                NotificationToken.builder()
                                        .member(owner)
                                        .fcmToken(fcmToken)
                                        .deviceKey(deviceKey)
                                        .userAgent(userAgent)
                                        .isActive(true)
                                        .lastSeenAt(now)
                                        .build()
                        )
                );
    }

    @Transactional
    public void deactivateToken(Long memberId, NotificationTokenDeactivateRequest request) {
        String fcmToken = normalizeRequired(request.fcmToken());
        notificationTokenRepository.findByMemberIdAndFcmToken(memberId, fcmToken)
                .ifPresent(token -> token.deactivate(Instant.now()));
    }

    @Transactional(readOnly = true)
    public NotificationToken findLatestActiveToken(Long memberId) {
        return notificationTokenRepository.findFirstByMemberIdAndIsActiveTrueOrderByUpdatedAtDesc(memberId)
                .orElse(null);
    }

    private void deactivateDuplicatedDeviceTokens(Long memberId, String deviceKey, String currentToken, Instant now) {
        if (!StringUtils.hasText(deviceKey)) {
            return;
        }
        List<NotificationToken> tokens =
                notificationTokenRepository.findAllByMemberIdAndDeviceKeyAndIsActiveTrue(memberId, deviceKey);

        for (NotificationToken token : tokens) {
            if (!token.getFcmToken().equals(currentToken)) {
                token.deactivate(now);
            }
        }
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeNullable(value, 4096);
        if (!StringUtils.hasText(normalized)) {
            throw new ApiException(NotificationErrorCode.INVALID_FCM_TOKEN);
        }
        return normalized;
    }

    private String normalizeNullable(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }
}
