package com.matchimban.matchimban_api.member.service;

import com.matchimban.matchimban_api.member.dto.response.MemberMeResponse;

public interface MemberService {
	MemberMeResponse getMyInfo(Long memberId);

	void withdraw(Long memberId);
}
