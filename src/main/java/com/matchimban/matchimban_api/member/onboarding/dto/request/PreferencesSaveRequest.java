package com.matchimban.matchimban_api.member.onboarding.dto.request;

import java.util.List;

public record PreferencesSaveRequest(
	List<String> allergyGroup,
	List<String> preferredCategories,
	List<String> dislikedCategories
) {
}
