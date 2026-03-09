package com.matchimban.matchimban_api.settlement.ocr.entity;

import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.ocr.enums.OcrJobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "settlement_ocr_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SettlementOcrJob {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settlement_ocr_jobs_seq_gen")
    @SequenceGenerator(name = "settlement_ocr_jobs_seq_gen", sequenceName = "settlement_ocr_jobs_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private MeetingSettlement settlement;

    @Column(name = "request_id", nullable = false, length = 80)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OcrJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 500)
    private String lastErrorMessage;

    @Builder.Default
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markProcessing(String lockedBy, Instant lockUntil, Instant now) {
        this.status = OcrJobStatus.PROCESSING;
        this.lockedBy = lockedBy;
        this.lockUntil = lockUntil;
        this.attemptCount += 1;

        if (this.startedAt == null) {
            this.startedAt = now;
        }
    }

    public void requeue(String code, String message, Instant nextAttemptAt) {
        this.status = OcrJobStatus.PENDING;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
        this.nextAttemptAt = nextAttemptAt;
        this.lockedBy = null;
        this.lockUntil = null;
    }

    public void markSucceeded(Instant now) {
        this.status = OcrJobStatus.SUCCEEDED;
        this.lockedBy = null;
        this.lockUntil = null;
        this.completedAt = now;
    }

    public void markFailed(String code, String message, Instant now) {
        this.status = OcrJobStatus.FAILED;
        this.lastErrorCode = code;
        this.lastErrorMessage = message;
        this.lockedBy = null;
        this.lockUntil = null;
        this.completedAt = now;
    }
}