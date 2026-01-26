package com.matchimban.matchimban_api.member.dto.response;

import java.util.List;

public record MemberPreferencesResponse(
	List<PreferenceItemResponse> allergyGroups,
	List<PreferenceItemResponse> preferredCategories,
	List<PreferenceItemResponse> dislikedCategories
) {
}
