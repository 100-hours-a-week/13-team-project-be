package com.matchimban.matchimban_api.member.repository;

import com.matchimban.matchimban_api.member.entity.FoodCategory;
import com.matchimban.matchimban_api.member.entity.enums.FoodCategoryType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodCategoryRepository extends JpaRepository<FoodCategory, Long> {
	List<FoodCategory> findByCategoryType(FoodCategoryType categoryType);
	List<FoodCategory> findByCategoryTypeAndCategoryCodeIn(
		FoodCategoryType categoryType,
		Collection<String> categoryCodes
	);
	Optional<FoodCategory> findByCategoryTypeAndCategoryCode(
		FoodCategoryType categoryType,
		String categoryCode
	);
}
