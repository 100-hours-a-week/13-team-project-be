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
            allocationSize = 1
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

//    @Column(name = "base_rank")
//    private Integer baseRank;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "ai_score", precision = 6, scale = 5)
    private BigDecimal aiScore;

    @Column(name = "final_rank")
    private Integer finalRank;

    private Integer likeCount;
    private Integer dislikeCount;
    private Integer neutralCount;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void applyCounts(int like, int dislike, int neutral) {
        this.likeCount = like;
        this.dislikeCount = dislike;
        this.neutralCount = neutral;
    }
    public void applyFinalRank(int finalRank) {
        this.finalRank = finalRank;
    }

}
