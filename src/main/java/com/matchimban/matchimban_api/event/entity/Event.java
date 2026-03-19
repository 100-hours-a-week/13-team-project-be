package com.matchimban.matchimban_api.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "events")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "events_seq_gen")
    @SequenceGenerator(
            name = "events_seq_gen",
            sequenceName = "events_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", length = 30, nullable = false)
    private CouponType couponType;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void increaseIssuedCount(int quantity) {
        this.issuedCount += quantity;
    }

    public void decreaseIssuedCount(int quantity) {
        this.issuedCount -= quantity;
        if (this.issuedCount < 0) {
            this.issuedCount = 0;
        }
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
    }
}