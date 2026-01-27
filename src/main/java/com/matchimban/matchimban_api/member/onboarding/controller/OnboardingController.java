package com.matchimban.matchimban_api.member.onboarding.controller;

import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.global.swagger.OnboardingErrorResponses;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.onboarding.dto.error.ConflictSelectionData;
import com.matchimban.matchimban_api.member.onboarding.dto.error.MissingAgreementsData;
import com.matchimban.matchimban_api.member.onboarding.dto.error.ValidationErrorData;
import com.matchimban.matchimban_api.member.onboarding.dto.request.AgreementConsentRequest;
import com.matchimban.matchimban_api.member.onboarding.dto.request.PreferencesSaveRequest;
import com.matchimban.matchimban_api.member.onboarding.dto.response.AgreementDetailResponse;
import com.matchimban.matchimban_api.member.onboarding.dto.response.AgreementListResponse;
import com.matchimban.matchimban_api.member.onboarding.dto.response.PreferencesChoicesResponse;
import com.matchimban.matchimban_api.member.onboarding.dto.response.PreferencesSavedData;
import com.matchimban.matchimban_api.member.onboarding.service.OnboardingService;
import com.matchimban.matchimban_api.member.onboarding.service.OnboardingService.AgreementConsentResult;
import com.matchimban.matchimban_api.member.onboarding.service.OnboardingService.PreferencesSaveResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Onboarding", description = "약관/취향 온보딩 API")
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

	private final OnboardingService onboardingService;
	private final JwtTokenProvider jwtTokenProvider;

	public OnboardingController(
		OnboardingService onboardingService,
		JwtTokenProvider jwtTokenProvider
	) {
		this.onboardingService = onboardingService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@GetMapping("/agreements")
	@Operation(summary = "약관 목록 조회", description = "필수 약관 목록을 반환한다.")
	@ApiResponse(
		responseCode = "200",
		description = "agreements_loaded"
	)
	@OnboardingErrorResponses
	public ResponseEntity<ApiResult<AgreementListResponse>> getAgreements() {
		AgreementListResponse response = onboardingService.getRequiredAgreements();
		return ResponseEntity.ok(ApiResult.of("agreements_loaded", response));
	}

	@GetMapping("/agreements/{agreementId}")
	@Operation(summary = "약관 전문 조회", description = "약관 전문을 반환한다.")
	@ApiResponse(
		responseCode = "200",
		description = "agreement_detail_loaded"
	)
	@OnboardingErrorResponses
	public ResponseEntity<ApiResult<AgreementDetailResponse>> getAgreementDetail(
		@PathVariable Long agreementId
	) {
		AgreementDetailResponse response = onboardingService.getAgreementDetail(agreementId);
		return ResponseEntity.ok(ApiResult.of("agreement_detail_loaded", response));
	}

	@PostMapping("/agreements/consent")
	@Operation(summary = "약관 동의 제출", description = "필수 약관 동의 시 ONBOARDING으로 전환한다.")
	@ApiResponse(
		responseCode = "200",
		description = "agreements_accepted"
	)
	@CsrfRequired
	@OnboardingErrorResponses
	public ResponseEntity<ApiResult<?>> consentAgreements(
		@RequestBody AgreementConsentRequest request
	) {
		// 약관 동의 후 상태 전환 및 JWT 재발급
		MemberPrincipal principal = requirePrincipal();
		AgreementConsentResult result = onboardingService.acceptAgreements(principal.memberId(), request);
		if (result.hasMissing()) {
			// 필수 약관 미동의면 400으로 누락 목록을 내려준다.
			MissingAgreementsData data = new MissingAgreementsData(result.missingAgreementIds());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResult.of("required_agreement_not_accepted", data));
		}

		Member member = result.member();
		HttpHeaders headers = new HttpHeaders();
		// 상태 전환이 반영된 최신 JWT를 재발급한다.
		String accessToken = jwtTokenProvider.createAccessToken(member, principal.sid());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(accessToken).toString());

		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("agreements_accepted"));
	}

	@GetMapping("/preferences/choices")
	@Operation(summary = "취향 선택지 조회", description = "알레르기 그룹 및 카테고리 선택지를 반환한다.")
	@ApiResponse(
		responseCode = "200",
		description = "preferences_options_loaded"
	)
	@OnboardingErrorResponses
	public ResponseEntity<ApiResult<PreferencesChoicesResponse>> getPreferenceChoices() {
		PreferencesChoicesResponse response = onboardingService.getPreferenceChoices();
		return ResponseEntity.ok(ApiResult.of("preferences_options_loaded", response));
	}

	@PostMapping("/preferences")
	@Operation(summary = "취향 저장", description = "취향 정보를 저장하고 ACTIVE로 전환한다.")
	@ApiResponse(
		responseCode = "200",
		description = "preferences_saved"
	)
	@CsrfRequired
	@OnboardingErrorResponses
	public ResponseEntity<ApiResult<?>> savePreferences(
		@RequestBody PreferencesSaveRequest request
	) {
		// 취향 저장 후 상태 전환 및 JWT 재발급
		MemberPrincipal principal = requirePrincipal();
		PreferencesSaveResult result = onboardingService.savePreferences(principal.memberId(), request);
		if (result.hasOverlaps()) {
			// 선호/비선호 중복은 409로 처리한다.
			ConflictSelectionData data = new ConflictSelectionData(result.overlappedCategories());
			return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiResult.of("conflict_selection", data));
		}
		if (result.hasFieldErrors()) {
			// 선택 코드 오류는 400으로 처리한다.
			ValidationErrorData data = new ValidationErrorData(result.fieldErrors());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResult.of("invalid_request", data));
		}

		Member member = result.member();
		HttpHeaders headers = new HttpHeaders();
		// 상태 전환이 반영된 최신 JWT를 재발급한다.
		String accessToken = jwtTokenProvider.createAccessToken(member, principal.sid());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(accessToken).toString());

		PreferencesSavedData data = new PreferencesSavedData(member.getStatus().name());
		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("preferences_saved", data));
	}

	private MemberPrincipal requirePrincipal() {
		// SecurityContext에서 로그인된 사용자만 허용
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
		}
		return principal;
	}
}
