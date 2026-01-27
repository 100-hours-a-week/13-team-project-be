package com.matchimban.matchimban_api.member.entity;

import com.matchimban.matchimban_api.member.entity.enums.PolicyType;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "policy",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_policy_type_version",
		columnNames = {"policy_type", "terms_version"}
	)
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Policy {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "policy_seq")
	@SequenceGenerator(name = "policy_seq", sequenceName = "policy_seq", allocationSize = 1)
	private Long id;

	@Column(name = "terms_content", columnDefinition = "TEXT", nullable = false)
	private String termsContent;

	@Enumerated(EnumType.STRING)
	@Column(name = "policy_type", length = 30, nullable = false)
	private PolicyType policyType;

	@Column(name = "title", length = 100, nullable = false)
	private String title;

	@Column(name = "terms_version", length = 10, nullable = false)
	private String termsVersion;

	@Column(name = "summary", columnDefinition = "TEXT")
	private String summary;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "is_required", nullable = false)
	private boolean isRequired;
}
