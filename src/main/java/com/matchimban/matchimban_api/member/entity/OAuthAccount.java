package com.matchimban.matchimban_api.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "oauth_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "oauth_accounts_seq")
	@SequenceGenerator(name = "oauth_accounts_seq", sequenceName = "oauth_accounts_seq", allocationSize = 1)
	private Long id;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "provider_user_id", length = 32, nullable = false)
	private String providerUserId;

	@Column(length = 10, nullable = false)
	private String provider;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
