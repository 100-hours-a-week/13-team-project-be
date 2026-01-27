package com.matchimban.matchimban_api.member.dto;

public record MemberCreateRequest(
	String nickname,
	String profileImageUrl,
	String thumbnailImageUrl
) {
}
