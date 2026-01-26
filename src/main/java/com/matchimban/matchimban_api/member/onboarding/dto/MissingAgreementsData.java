package com.matchimban.matchimban_api.member.onboarding.dto;

import java.util.List;

public record MissingAgreementsData(
	List<Long> missingAgreementIds
) {
}
