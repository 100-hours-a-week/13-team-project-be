package com.matchimban.matchimban_api.auth.oauth.model;

public record OAuthUserInfo(
	String providerMemberId,
	String nickname,
	String thumbnailImageUrl,
	String profileImageUrl
) {
}

