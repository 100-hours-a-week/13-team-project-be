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
@Table(name = "notification_schedule_jobs")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationScheduleJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    private NotificationJobType jobType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "run_at", nullable = false)
    private Instant runAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationScheduleJobStatus status = NotificationScheduleJobStatus.PENDING;

    @Column(name = "job_key", nullable = false, length = 120)
    private String jobKey;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "worker_id", length = 64)
    private String workerId;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void claim(String workerId, Instant now) {
        this.status = NotificationScheduleJobStatus.IN_PROGRESS;
        this.lockedAt = now;
        this.workerId = workerId;
        this.attemptCount += 1;
    }

    public void markDone() {
        this.status = NotificationScheduleJobStatus.DONE;
        this.lockedAt = null;
        this.workerId = null;
        this.lastErrorMessage = null;
    }

    public void markFailed(String message, Instant nextAttemptAt) {
        this.status = NotificationScheduleJobStatus.FAILED;
        this.lastErrorMessage = message;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedAt = null;
        this.workerId = null;
    }

    public void markDead(String message, Instant now) {
        this.status = NotificationScheduleJobStatus.DEAD;
        this.lastErrorMessage = message;
        this.nextAttemptAt = now;
        this.lockedAt = null;
        this.workerId = null;
    }

    public boolean hasReachedAttemptLimit(int maxAttempts) {
        return this.attemptCount >= maxAttempts;
    }
}
