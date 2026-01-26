package com.matchimban.matchimban_api.member.onboarding.dto;

import java.util.List;

public record PreferencesChoicesResponse(
	List<PreferenceOption> allergyGroups,
	List<PreferenceOption> categories
) {
}
