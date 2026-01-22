package com.matchimban.matchimban_api.member.entity;

import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "members_seq")
	@SequenceGenerator(name = "members_seq", sequenceName = "members_seq", allocationSize = 1)
	private Long id;

	@Column(length = 30)
	private String nickname;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Column(name = "thumbnail_image_url", length = 500)
	private String thumbnailImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private MemberStatus status;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "is_guest", nullable = false)
	private boolean isGuest;

	@Column(name = "guest_uuid")
	private UUID guestUuid;
}
