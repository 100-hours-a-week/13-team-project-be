package com.matchimban.matchimban_api.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "settlement_item_selections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SettlementItemSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settlement_item_selections_seq_gen")
    @SequenceGenerator(name = "settlement_item_selections_seq_gen", sequenceName = "settlement_item_selections_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ReceiptItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_participant_id", nullable = false)
    private SettlementParticipant settlementParticipant;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}