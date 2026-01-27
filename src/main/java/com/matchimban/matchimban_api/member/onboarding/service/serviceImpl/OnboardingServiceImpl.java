package com.matchimban.matchimban_api.member.onboarding.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.member.entity.FoodCategory;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.MemberAgreement;
import com.matchimban.matchimban_api.member.entity.MemberCategoryMapping;
import com.matchimban.matchimban_api.member.entity.Policy;
import com.matchimban.matchimban_api.member.entity.enums.FoodCategoryType;
import com.matchimban.matchimban_api.member.entity.enums.MemberCategoryRelationType;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import com.matchimban.matchimban_api.member.onboarding.dto.error.FieldErrorData;
import com.matchimban.matchimban_api.member.onboarding.dto.request.AgreementConsentRequest;
import com.matchimban.matchimban_api.member.onboarding.dto.request.AgreementConsentRequestItem;
import com.matchimban.matchimban_api.member.onboarding.dto.request.PreferencesSaveRequest;
import com.matchimban.matchimban_api.member.onboarding.dto.response.AgreementDetailResponse;
import com.matchimban.matchimban_api.member.onboarding.dto.response.AgreementListItem;
import com.matchimban.matchimban_api.member.onboarding.dto.response.AgreementListResponse;
import com.matchimban.matchimban_api.member.onboarding.dto.response.PreferenceOption;
import com.matchimban.matchimban_api.member.onboarding.dto.response.PreferencesChoicesResponse;
import com.matchimban.matchimban_api.member.onboarding.service.OnboardingService;
import com.matchimban.matchimban_api.member.repository.FoodCategoryRepository;
import com.matchimban.matchimban_api.member.repository.MemberAgreementRepository;
import com.matchimban.matchimban_api.member.repository.MemberCategoryMappingRepository;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import com.matchimban.matchimban_api.member.repository.PolicyRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingServiceImpl implements OnboardingService {

	private final PolicyRepository policyRepository;
	private final MemberAgreementRepository memberAgreementRepository;
	private final MemberRepository memberRepository;
	private final FoodCategoryRepository foodCategoryRepository;
	private final MemberCategoryMappingRepository memberCategoryMappingRepository;

	public OnboardingServiceImpl(
		PolicyRepository policyRepository,
		MemberAgreementRepository memberAgreementRepository,
		MemberRepository memberRepository,
		FoodCategoryRepository foodCategoryRepository,
		MemberCategoryMappingRepository memberCategoryMappingRepository
	) {
		this.policyRepository = policyRepository;
		this.memberAgreementRepository = memberAgreementRepository;
		this.memberRepository = memberRepository;
		this.foodCategoryRepository = foodCategoryRepository;
		this.memberCategoryMappingRepository = memberCategoryMappingRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public AgreementListResponse getRequiredAgreements() {
		List<Policy> requiredPolicies = policyRepository.findByIsRequiredTrue();
		List<AgreementListItem> items = requiredPolicies.stream()
			.map(this::toAgreementItem)
			.toList();
		return new AgreementListResponse(items);
	}

	@Override
	@Transactional(readOnly = true)
	public AgreementDetailResponse getAgreementDetail(Long agreementId) {
		Policy policy = policyRepository.findById(agreementId)
			.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "invalid_request"));
		return new AgreementDetailResponse(policy.getId(), policy.getTitle(), policy.getTermsContent());
	}

	@Override
	@Transactional
	public AgreementConsentResult acceptAgreements(Long memberId, AgreementConsentRequest request) {
		if (request == null || request.agreements() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request");
		}

		// 필수 약관 미동의가 있는지 먼저 확인한다.
		List<AgreementConsentRequestItem> agreements = request.agreements();
		Map<Long, Boolean> consentMap = new HashMap<>();
		for (AgreementConsentRequestItem item : agreements) {
			if (item == null || item.agreementId() == null) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request");
			}
			consentMap.put(item.agreementId(), item.agreed());
		}

		List<Policy> requiredPolicies = policyRepository.findByIsRequiredTrue();
		List<Long> missingRequired = requiredPolicies.stream()
			.map(Policy::getId)
			.filter(id -> !Boolean.TRUE.equals(consentMap.get(id)))
			.toList();

		if (!missingRequired.isEmpty()) {
			// 필수 약관 누락 시 상태 변경 없이 목록만 반환한다.
			return new AgreementConsentResult(missingRequired, null);
		}

		Set<Long> agreementIds = agreements.stream()
			.map(AgreementConsentRequestItem::agreementId)
			.collect(Collectors.toSet());

		List<Policy> policies = policyRepository.findAllById(agreementIds);
		if (policies.size() != agreementIds.size()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request");
		}

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized"));

		// 동의 내역 저장
		LocalDateTime now = LocalDateTime.now();
		List<MemberAgreement> toSave = new ArrayList<>();
		for (Policy policy : policies) {
			if (!Boolean.TRUE.equals(consentMap.get(policy.getId()))) {
				continue;
			}
			if (memberAgreementRepository.existsByMemberIdAndPolicyId(memberId, policy.getId())) {
				continue;
			}
			toSave.add(MemberAgreement.builder()
				.member(member)
				.policy(policy)
				.acceptedAt(now)
				.build());
		}
		if (!toSave.isEmpty()) {
			memberAgreementRepository.saveAll(toSave);
		}

		if (member.getStatus() == MemberStatus.PENDING) {
			// 약관 동의 완료 시 PENDING -> ONBOARDING 전환.
			member.updateStatus(MemberStatus.ONBOARDING);
			memberRepository.save(member);
		}

		return new AgreementConsentResult(List.of(), member);
	}

	@Override
	@Transactional(readOnly = true)
	public PreferencesChoicesResponse getPreferenceChoices() {
		List<PreferenceOption> allergyGroups = foodCategoryRepository
			.findByCategoryType(FoodCategoryType.ALLERGY_GROUP)
			.stream()
			.map(category -> new PreferenceOption(
				category.getCategoryCode(),
				category.getCategoryName(),
				category.getEmoji()
			))
			.toList();

		List<PreferenceOption> categories = foodCategoryRepository
			.findByCategoryType(FoodCategoryType.CATEGORY)
			.stream()
			.map(category -> new PreferenceOption(
				category.getCategoryCode(),
				category.getCategoryName(),
				category.getEmoji()
			))
			.toList();

		return new PreferencesChoicesResponse(allergyGroups, categories);
	}

	@Override
	@Transactional
	public PreferencesSaveResult savePreferences(Long memberId, PreferencesSaveRequest request) {
		if (request == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request");
		}

		List<String> allergyCodes = safeList(request.allergyGroup());
		List<String> preferredCodes = safeList(request.preferredCategories());
		List<String> dislikedCodes = safeList(request.dislikedCategories());

		// 선호/비선호 중복은 허용하지 않는다.
		Set<String> overlap = new HashSet<>(preferredCodes);
		overlap.retainAll(dislikedCodes);
		if (!overlap.isEmpty()) {
			return new PreferencesSaveResult(
				List.of(),
				overlap.stream().sorted().toList(),
				null
			);
		}

		// 선택 코드 검증
		ValidationResult allergyValidation = validateCodes(allergyCodes, FoodCategoryType.ALLERGY_GROUP, "allergy_group");
		ValidationResult preferredValidation = validateCodes(preferredCodes, FoodCategoryType.CATEGORY, "preferred_categories");
		ValidationResult dislikedValidation = validateCodes(dislikedCodes, FoodCategoryType.CATEGORY, "disliked_categories");

		List<FieldErrorData> errors = new ArrayList<>();
		errors.addAll(allergyValidation.errors());
		errors.addAll(preferredValidation.errors());
		errors.addAll(dislikedValidation.errors());

		if (!errors.isEmpty()) {
			// 입력 코드 오류가 있으면 상태 변경 없이 에러 목록만 반환한다.
			return new PreferencesSaveResult(errors, List.of(), null);
		}

		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized"));

		// 기존 매핑을 전부 교체한다.
		memberCategoryMappingRepository.deleteByMemberId(memberId);
		List<MemberCategoryMapping> mappings = new ArrayList<>();
		appendMappings(mappings, member, allergyValidation.categories(), MemberCategoryRelationType.ALLERGY);
		appendMappings(mappings, member, preferredValidation.categories(), MemberCategoryRelationType.PREFERENCE);
		appendMappings(mappings, member, dislikedValidation.categories(), MemberCategoryRelationType.DISLIKE);

		if (!mappings.isEmpty()) {
			memberCategoryMappingRepository.saveAll(mappings);
		}

		if (member.getStatus() == MemberStatus.ONBOARDING) {
			// 취향 입력 완료 시 ONBOARDING -> ACTIVE 전환.
			member.updateStatus(MemberStatus.ACTIVE);
			memberRepository.save(member);
		}

		return new PreferencesSaveResult(List.of(), List.of(), member);
	}

	private AgreementListItem toAgreementItem(Policy policy) {
		return new AgreementListItem(
			policy.getId(),
			policy.getPolicyType().name(),
			policy.getTitle(),
			policy.getTermsVersion(),
			policy.isRequired(),
			splitSummary(policy.getSummary())
		);
	}

	private List<String> splitSummary(String summary) {
		if (summary == null || summary.isBlank()) {
			return List.of();
		}
		return List.of(summary.split("\\n"));
	}

	private List<String> safeList(List<String> values) {
		return values == null ? List.of() : values;
	}

	private ValidationResult validateCodes(List<String> codes, FoodCategoryType type, String field) {
		if (codes.isEmpty()) {
			return new ValidationResult(List.of(), List.of());
		}
		List<FoodCategory> categories = foodCategoryRepository.findByCategoryTypeAndCategoryCodeIn(type, codes);
		Set<String> foundCodes = categories.stream()
			.map(FoodCategory::getCategoryCode)
			.collect(Collectors.toSet());

		List<FieldErrorData> errors = new ArrayList<>();
		for (String code : codes) {
			if (!foundCodes.contains(code)) {
				errors.add(new FieldErrorData(field, "unsupported_code"));
				break;
			}
		}

		return new ValidationResult(errors, categories);
	}

	private void appendMappings(
		List<MemberCategoryMapping> mappings,
		Member member,
		List<FoodCategory> categories,
		MemberCategoryRelationType relationType
	) {
		for (FoodCategory category : categories) {
			mappings.add(MemberCategoryMapping.builder()
				.member(member)
				.category(category)
				.relationType(relationType)
				.build());
		}
	}

	private record ValidationResult(
		List<FieldErrorData> errors,
		List<FoodCategory> categories
	) {
	}
}
