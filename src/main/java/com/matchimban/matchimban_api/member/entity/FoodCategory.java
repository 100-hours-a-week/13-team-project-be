package com.matchimban.matchimban_api.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "food_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodCategory {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "food_category_seq")
	@SequenceGenerator(name = "food_category_seq", sequenceName = "food_category_seq", allocationSize = 1)
	private Long id;

	@Column(name = "category_name", length = 30)
	private String categoryName;
}
