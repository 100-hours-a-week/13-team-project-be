package com.matchimban.matchimban_api.member.onboarding.dto.response;

import java.util.List;

public record AgreementListItem(
	Long agreementId,
	String type,
	String title,
	String version,
	boolean isRequired,
	List<String> summary
) {
}
