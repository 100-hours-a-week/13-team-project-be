package com.matchimban.matchimban_api.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "receipt_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "receipt_items_seq_gen")
    @SequenceGenerator(name = "receipt_items_seq_gen", sequenceName = "receipt_items_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private MeetingSettlement settlement;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void update(String name, java.math.BigDecimal unitPrice, Integer quantity, java.math.BigDecimal totalPrice) {
        this.itemName = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }
}