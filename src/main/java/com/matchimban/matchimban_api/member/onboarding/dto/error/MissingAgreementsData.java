package com.matchimban.matchimban_api.member.onboarding.dto.error;

import java.util.List;

public record MissingAgreementsData(
	List<Long> missingAgreementIds
) {
}
