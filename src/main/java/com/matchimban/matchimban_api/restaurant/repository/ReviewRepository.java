package com.matchimban.matchimban_api.restaurant.repository;

import com.matchimban.matchimban_api.restaurant.entity.Review;
import java.util.List;
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
}
