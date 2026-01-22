package com.matchimban.matchimban_api.auth.kakao.service;

import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;
import com.matchimban.matchimban_api.member.entity.Member;

public interface KakaoMemberService {
	Member findOrCreateMember(KakaoUserInfo userInfo);
}
