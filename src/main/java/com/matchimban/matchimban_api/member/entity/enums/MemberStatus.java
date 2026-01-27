package com.matchimban.matchimban_api.member.entity.enums;

public enum MemberStatus {
	PENDING,//(약관 미완료 / 가입 미완료 상태 Oauth 연동만 된 상태)
	ACTIVE, // 성공적으로 회원가입을 한 상태
    ONBOARDING, //취향을 입력하지 않은 상태 (약관 동의는 한 상태)
	DELETED // 탈퇴한 상태
}
