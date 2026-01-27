package com.matchimban.matchimban_api.member.onboarding.dto.error;

import java.util.List;

public record ConflictSelectionData(
	List<String> overlappedCategories
) {
}
