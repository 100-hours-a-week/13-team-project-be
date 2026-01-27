package com.matchimban.matchimban_api.vote.entity;

import com.matchimban.matchimban_api.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_restaurant_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MeetingRestaurantCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "meeting_restaurant_candidates_seq_gen")
    @SequenceGenerator(
            name = "meeting_restaurant_candidates_seq_gen",
            sequenceName = "meeting_restaurant_candidates_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id")
    private Vote vote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "base_rank")
    private Integer baseRank;

    private BigDecimal rating;

    @Column(name = "result_rank")
    private Integer resultRank;

    private Integer likeCount;
    private Integer dislikeCount;
    private Integer neutralCount;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
