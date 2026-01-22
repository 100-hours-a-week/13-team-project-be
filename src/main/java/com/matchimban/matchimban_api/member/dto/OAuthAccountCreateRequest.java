package com.matchimban.matchimban_api.member.dto;

import com.matchimban.matchimban_api.member.entity.Member;

public record OAuthAccountCreateRequest(
	String provider,
	String providerMemberId,
	Member member
) {
}
