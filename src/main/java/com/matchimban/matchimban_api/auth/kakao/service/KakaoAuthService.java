package com.matchimban.matchimban_api.auth.kakao.service;

import com.matchimban.matchimban_api.auth.kakao.dto.KakaoTokenResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;

public interface KakaoAuthService {
	String issueState();

	boolean consumeState(String state);

	String buildAuthorizeUrl(String state);

	KakaoTokenResponse requestToken(String code);

	KakaoUserInfo requestUserInfo(String accessToken);

}
