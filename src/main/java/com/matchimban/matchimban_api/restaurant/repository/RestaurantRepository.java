package com.matchimban.matchimban_api.restaurant.repository;

import com.matchimban.matchimban_api.restaurant.entity.Restaurant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByIdIn(List<Long> ids);
}
