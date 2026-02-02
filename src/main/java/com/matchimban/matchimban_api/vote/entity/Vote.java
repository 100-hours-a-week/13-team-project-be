package com.matchimban.matchimban_api.vote.entity;

import com.matchimban.matchimban_api.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "votes_seq_gen")
    @SequenceGenerator(name = "votes_seq_gen", sequenceName = "votes_seq", allocationSize = 1)
    private Long id;

    private int round;

    @Enumerated(EnumType.STRING)
    @Column(name = "state",length = 20)
    private VoteStatus status;

    private Instant generatedAt;
    private Instant countedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private Meeting meeting;

    public void markGenerating() {
        this.status = VoteStatus.GENERATING;
        this.generatedAt = null;
        this.countedAt = null;
    }

    public void markOpen(Instant generatedAt) {
        this.status = VoteStatus.OPEN;
        this.generatedAt = generatedAt;
    }

    public void markReserved(Instant generatedAt) {
        this.status = VoteStatus.RESERVED;
        this.generatedAt = generatedAt;
    }

    public void markCounting() {
        this.status = VoteStatus.COUNTING;
    }

    public void markCounted(Instant countedAt) {
        this.status = VoteStatus.COUNTED;
        this.countedAt = countedAt;
    }

    public void markFailed() {
        this.status = VoteStatus.FAILED;
    }
}
