package com.matchimban.matchimban_api.member.dto.response;

import java.time.Instant;

public record MemberMeResponse(
	Long memberId,
	String nickname,
	String profileImageUrl,
	String thumbnailImageUrl,
	String status,
	Instant createdAt,
	Instant updatedAt,
	MemberPreferencesResponse preferences
) {
}
