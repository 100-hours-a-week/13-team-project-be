package com.matchimban.matchimban_api.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "restaurants_seq_gen")
    @SequenceGenerator(name = "restaurants_seq_gen", sequenceName = "restaurants_seq", allocationSize = 50)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    private String name;

    private BigDecimal lat;
    private BigDecimal lng;

    private String phone;

    @Column(name = "address_road")
    private String addressRoad;

    @Column(name = "category_original")
    private String categoryOriginal;

    private String status;

    private Integer reviewCountVisitor;
    private Integer reviewCountBlog;

    private LocalTime openTime;
    private LocalTime closeTime;
    private LocalTime breakStart;
    private LocalTime breakEnd;
    private LocalTime lastOrderTime;

    @Column(name = "place_feature_hash")
    private String placeFeatureHash;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
