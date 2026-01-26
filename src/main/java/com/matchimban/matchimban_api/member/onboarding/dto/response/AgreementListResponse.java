package com.matchimban.matchimban_api.member.onboarding.dto.response;

import java.util.List;

public record AgreementListResponse(
	List<AgreementListItem> required
) {
}
