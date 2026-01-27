package com.matchimban.matchimban_api.member.entity;

import com.matchimban.matchimban_api.member.entity.enums.FoodCategoryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "food_category",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_food_category_code_type",
		columnNames = {"category_code", "category_type"}
	)
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FoodCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "food_category_seq")
	@SequenceGenerator(name = "food_category_seq", sequenceName = "food_category_seq", allocationSize = 1)
	private Long id;

	@Column(name = "category_code", length = 30, nullable = false)
	private String categoryCode;

	@Column(name = "category_name", length = 30, nullable = false)
	private String categoryName;

	@Column(name = "emoji", length = 10)
	private String emoji;

	@Enumerated(EnumType.STRING)
	@Column(name = "category_type", length = 20, nullable = false)
	private FoodCategoryType categoryType;
}
