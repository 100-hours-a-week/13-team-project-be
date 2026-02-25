package com.matchimban.matchimban_api.settlement.entity;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SettlementParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settlement_participants_seq_gen")
    @SequenceGenerator(name = "settlement_participants_seq_gen", sequenceName = "settlement_participants_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private MeetingSettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private MeetingParticipant participant;

    @Column(name = "subtotal_amount", precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_allocated_amount", precision = 12, scale = 2)
    private BigDecimal discountAllocatedAmount;

    @Column(name = "amount_due", precision = 12, scale = 2)
    private BigDecimal amountDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "selection_confirmed_at")
    private Instant selectionConfirmedAt;

    @Column(name = "payment_requested_at")
    private Instant paymentRequestedAt;

    @Column(name = "payment_confirmed_at")
    private Instant paymentConfirmedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isSelectionConfirmed() {
        return selectionConfirmedAt != null;
    }

    public void markSelectionConfirmed(Instant now) {
        this.selectionConfirmedAt = now;
    }

    public void requestPayment(Instant now) {
        this.paymentStatus = PaymentStatus.REQUESTED;
        this.paymentRequestedAt = now;
    }

    public void confirmPayment(Instant now) {
        this.paymentStatus = PaymentStatus.DONE;
        this.paymentConfirmedAt = now;
    }

    public void updateAmounts(java.math.BigDecimal subtotal, java.math.BigDecimal discountAllocated, java.math.BigDecimal amountDue) {
        this.subtotalAmount = subtotal;
        this.discountAllocatedAmount = discountAllocated;
        this.amountDue = amountDue;
    }
}