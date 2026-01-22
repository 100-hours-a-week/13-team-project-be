package com.matchimban.matchimban_api.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
	@SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 1)
	private Long id;

	@Column(length = 30)
	private String nickname;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Column(name = "thumbnail_image_url", length = 500)
	private String thumbnailImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private UserStatus status;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "is_guest", nullable = false)
	private boolean isGuest;

	@Column(name = "guest_uuid")
	private UUID guestUuid;
}
