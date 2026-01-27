package com.matchimban.matchimban_api.member.entity;

import com.matchimban.matchimban_api.member.entity.enums.MemberCategoryRelationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_category_mapping")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberCategoryMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_category_mapping_seq")
	@SequenceGenerator(
		name = "member_category_mapping_seq",
		sequenceName = "member_category_mapping_seq",
		allocationSize = 1
	)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "relation_type", length = 10)
	private MemberCategoryRelationType relationType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private FoodCategory category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;
}
