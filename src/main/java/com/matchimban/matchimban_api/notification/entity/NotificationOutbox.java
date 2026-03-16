package com.matchimban.matchimban_api.notification.entity;

import com.matchimban.matchimban_api.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "notification_outbox")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_token_id")
    private NotificationToken notificationToken;

    @Column(name = "token_snapshot", columnDefinition = "TEXT")
    private String tokenSnapshot;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationOutboxStatus status = NotificationOutboxStatus.PENDING;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Builder.Default
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "worker_id", length = 64)
    private String workerId;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void claim(String workerId, Instant now) {
        this.status = NotificationOutboxStatus.IN_PROGRESS;
        this.lockedAt = now;
        this.workerId = workerId;
        this.attemptCount += 1;
    }

    public void markSent(Instant now) {
        this.status = NotificationOutboxStatus.SENT;
        this.sentAt = now;
        this.lockedAt = null;
        this.workerId = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void markFailed(String code, String message, Instant nextAttemptAt) {
        this.status = NotificationOutboxStatus.FAILED;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedAt = null;
        this.workerId = null;
    }

    public void markDead(String code, String message, Instant now) {
        this.status = NotificationOutboxStatus.DEAD;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
        this.nextAttemptAt = now;
        this.lockedAt = null;
        this.workerId = null;
    }

    public boolean hasReachedAttemptLimit(int maxAttempts) {
        return this.attemptCount >= maxAttempts;
    }
}
