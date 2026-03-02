package com.matchimban.matchimban_api.restaurant.repository;

import com.matchimban.matchimban_api.restaurant.entity.Review;
import java.util.List;
import java.util.Optional;

import com.matchimban.matchimban_api.restaurant.repository.projection.MyReviewRow;
import com.matchimban.matchimban_api.restaurant.repository.projection.ReviewDetailRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    interface RestaurantAvgRatingRow {
        Long getRestaurantId();
        Double getAvgRating();
    }

    @Query("""
        select r.restaurant.id as restaurantId,
               avg(r.rating) as avgRating
        from Review r
        where r.restaurant.id in :restaurantIds
          and r.isDeleted = false
        group by r.restaurant.id
    """)
    List<RestaurantAvgRatingRow> findAvgRatingsByRestaurantIds(@Param("restaurantIds") List<Long> restaurantIds);

    @Query("""
        select (count(r) > 0)
        from Review r
        where r.meeting.id = :meetingId
          and r.member.id = :memberId
          and r.isDeleted = false
    """)
    boolean existsActiveReview(
            @Param("meetingId") Long meetingId,
            @Param("memberId") Long memberId
    );

    @Query("""
        select r
        from Review r
        where r.id = :reviewId
          and r.member.id = :memberId
          and r.isDeleted = false
    """)
    Optional<Review> findActiveByIdAndMemberId(
            @Param("reviewId") Long reviewId,
            @Param("memberId") Long memberId
    );

    @Query("""
        select new com.matchimban.matchimban_api.restaurant.repository.projection.MyReviewRow(
            r.id,
            r.meeting.id,
            r.rating,
            r.content,
            r.createdAt,
            res.id,
            res.name,
            res.imageUrl1,
            fc.categoryName,
            fc.emoji
        )
        from Review r
        join r.restaurant res
        join res.foodCategory fc
        where r.member.id = :memberId
          and r.isDeleted = false
          and (:cursor is null or r.id < :cursor)
        order by r.id desc
    """)
    List<MyReviewRow> findMyReviewRows(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
        select new com.matchimban.matchimban_api.restaurant.repository.projection.ReviewDetailRow(
            r.id,
            r.meeting.id,
            r.rating,
            r.content,
            r.createdAt,
            r.updatedAt,
            res.id,
            res.name,
            res.imageUrl1,
            fc.categoryName,
            fc.emoji
        )
        from Review r
        join r.restaurant res
        join res.foodCategory fc
        where r.id = :reviewId
          and r.member.id = :memberId
          and r.isDeleted = false
    """)
    Optional<ReviewDetailRow> findMyReviewDetailRow(
            @Param("reviewId") Long reviewId,
            @Param("memberId") Long memberId
    );
}
