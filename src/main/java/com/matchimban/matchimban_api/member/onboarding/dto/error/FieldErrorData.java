package com.matchimban.matchimban_api.member.onboarding.dto.error;

public record FieldErrorData(
	String field,
	String reason
) {
}
