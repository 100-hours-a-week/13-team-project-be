package com.matchimban.matchimban_api.member.dto.response;

import java.time.LocalDateTime;

public record MemberMeResponse(
	Long memberId,
	String nickname,
	String profileImageUrl,
	String thumbnailImageUrl,
	String status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	MemberPreferencesResponse preferences
) {
}
