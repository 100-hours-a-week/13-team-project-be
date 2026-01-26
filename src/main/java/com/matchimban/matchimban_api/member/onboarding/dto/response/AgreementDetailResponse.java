package com.matchimban.matchimban_api.member.onboarding.dto.response;

public record AgreementDetailResponse(
	Long agreementId,
	String title,
	String content
) {
}
