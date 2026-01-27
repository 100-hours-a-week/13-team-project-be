package com.matchimban.matchimban_api.member.onboarding.service;

import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.onboarding.dto.error.FieldErrorData;
import com.matchimban.matchimban_api.member.onboarding.dto.request.AgreementConsentRequest;
import com.matchimban.matchimban_api.member.onboarding.dto.request.PreferencesSaveRequest;
import com.matchimban.matchimban_api.member.onboarding.dto.response.AgreementDetailResponse;
import com.matchimban.matchimban_api.member.onboarding.dto.response.AgreementListResponse;
import com.matchimban.matchimban_api.member.onboarding.dto.response.PreferencesChoicesResponse;
import java.util.List;

public interface OnboardingService {

	AgreementListResponse getRequiredAgreements();

	AgreementDetailResponse getAgreementDetail(Long agreementId);

	AgreementConsentResult acceptAgreements(Long memberId, AgreementConsentRequest request);

	PreferencesChoicesResponse getPreferenceChoices();

	PreferencesSaveResult savePreferences(Long memberId, PreferencesSaveRequest request);

	record AgreementConsentResult(
		List<Long> missingAgreementIds,
		Member member
	) {
		public boolean hasMissing() {
			return !missingAgreementIds.isEmpty();
		}
	}

	record PreferencesSaveResult(
		List<FieldErrorData> fieldErrors,
		List<String> overlappedCategories,
		Member member
	) {
		public boolean hasFieldErrors() {
			return !fieldErrors.isEmpty();
		}

		public boolean hasOverlaps() {
			return !overlappedCategories.isEmpty();
		}
	}
}
