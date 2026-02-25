package com.matchimban.matchimban_api.settlement.entity;

import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "meeting_settlements",
        uniqueConstraints = @UniqueConstraint(name = "uq_meeting_settlements_meeting", columnNames = "meeting_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MeetingSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_settlements_seq_gen")
    @SequenceGenerator(name = "meeting_settlements_seq_gen", sequenceName = "meeting_settlements_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "receipt_image_url", length = 500)
    private String receiptImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", nullable = false, length = 30)
    private SettlementStatus settlementStatus;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void changeStatus(SettlementStatus next) {
        this.settlementStatus = next;
    }

    public void attachReceiptImageUrl(String receiptImageUrl) {
        this.receiptImageUrl = receiptImageUrl;
    }

    public void applyOcrSummary(java.math.BigDecimal totalAmount, java.math.BigDecimal discountAmount) {
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
    }
}