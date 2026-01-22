package com.matchimban.matchimban_api.auth.kakao.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record KakaoUserInfo(
	JsonNode raw,
	Long id,
	String nickname,
	String thumbnailImageUrl,
	String profileImageUrl
) {}
