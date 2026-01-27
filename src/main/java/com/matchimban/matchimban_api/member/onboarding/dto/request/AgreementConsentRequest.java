package com.matchimban.matchimban_api.member.onboarding.dto.request;

import java.util.List;

public record AgreementConsentRequest(
	List<AgreementConsentRequestItem> agreements
) {
}
