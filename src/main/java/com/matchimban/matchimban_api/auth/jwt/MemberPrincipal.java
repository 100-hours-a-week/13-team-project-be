package com.matchimban.matchimban_api.auth.jwt;

import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;

public record MemberPrincipal(
	Long memberId,
	MemberStatus status,
	String sid
) {
}
