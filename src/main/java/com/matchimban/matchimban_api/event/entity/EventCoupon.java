package com.matchimban.matchimban_api.event.entity;

import com.matchimban.matchimban_api.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "event_coupons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_event_coupons_event_participant",
                        columnNames = {"event_participant_id"}
                )
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class EventCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_coupons_seq_gen")
    @SequenceGenerator(
            name = "event_coupons_seq_gen",
            sequenceName = "event_coupons_seq",
            allocationSize = 1
    )
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_participant_id", nullable = false)
    private EventParticipant eventParticipant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", length = 30, nullable = false)
    private CouponType couponType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EventCouponStatus status;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

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

    public void markUsed(Instant usedAt) {
        this.status = EventCouponStatus.USED;
        this.usedAt = usedAt;
    }

    public void markExpired() {
        this.status = EventCouponStatus.EXPIRED;
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
    }
}