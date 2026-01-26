package com.matchimban.matchimban_api.member.onboarding.dto;

public record AgreementConsentRequestItem(
	Long agreementId,
	boolean agreed
) {
}
