package com.matchimban.matchimban_api.member.onboarding.dto.request;

public record AgreementConsentRequestItem(
	Long agreementId,
	boolean agreed
) {
}
