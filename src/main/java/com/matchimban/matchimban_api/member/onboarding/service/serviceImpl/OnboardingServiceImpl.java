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

import java.time.Instant;
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
		Instant now = Instant.now();
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

		// 기존 매핑을 조회해 변경분만 반영한다.
		List<MemberCategoryMapping> existingMappings = memberCategoryMappingRepository
			.findByMemberIdWithCategory(memberId); // 기존 매핑과 카테고리 정보를 함께 조회한다.

		Map<MappingKey, MemberCategoryMapping> existingByKey = new HashMap<>(); // 기존 매핑을 비교 키로 매핑한다.
		for (MemberCategoryMapping mapping : existingMappings) { // 기존 매핑을 순회한다.
			MappingKey key = new MappingKey(mapping.getCategory().getId(), mapping.getRelationType()); // 비교용 키를 만든다.
			existingByKey.put(key, mapping); // 키로 기존 매핑을 저장한다.
		}

		Set<MappingKey> requestedKeys = new HashSet<>(); // 요청된 매핑 키 집합을 만든다.
		Map<Long, FoodCategory> requestedCategoriesById = new HashMap<>(); // 요청된 카테고리 ID 기준 맵을 만든다.
		addRequestedKeys(requestedKeys, requestedCategoriesById, allergyValidation.categories(),
			MemberCategoryRelationType.ALLERGY); // 알레르기 매핑 키를 추가한다.
		addRequestedKeys(requestedKeys, requestedCategoriesById, preferredValidation.categories(),
			MemberCategoryRelationType.PREFERENCE); // 선호 매핑 키를 추가한다.
		addRequestedKeys(requestedKeys, requestedCategoriesById, dislikedValidation.categories(),
			MemberCategoryRelationType.DISLIKE); // 비선호 매핑 키를 추가한다.

		Set<MappingKey> toDeleteKeys = new HashSet<>(existingByKey.keySet()); // 삭제 대상 계산을 위한 복사본을 만든다.
		toDeleteKeys.removeAll(requestedKeys); // 기존 - 요청 = 삭제 대상이다.
		Set<MappingKey> toInsertKeys = new HashSet<>(requestedKeys); // 삽입 대상 계산을 위한 복사본을 만든다.
		toInsertKeys.removeAll(existingByKey.keySet()); // 요청 - 기존 = 삽입 대상이다.

		if (!toDeleteKeys.isEmpty()) { // 삭제 대상이 있는지 확인한다.
			List<Long> deleteIds = toDeleteKeys.stream() // 삭제할 ID 목록을 만든다.
				.map(key -> existingByKey.get(key).getId()) // 키로 기존 매핑 ID를 찾는다.
				.toList(); // ID 리스트로 수집한다.
			memberCategoryMappingRepository.deleteAllByIdInBatch(deleteIds); // 삭제 SQL을 즉시 실행한다.
			memberCategoryMappingRepository.flush(); // 삭제를 먼저 반영해 유니크 키 충돌을 방지한다.
		}

		List<MemberCategoryMapping> mappingsToInsert = new ArrayList<>(); // 삽입할 매핑 목록을 만든다.
		for (MappingKey key : toInsertKeys) { // 삽입 대상 키를 순회한다.
			FoodCategory category = requestedCategoriesById.get(key.categoryId()); // 카테고리 엔티티를 찾는다.
			if (category == null) { // 카테고리가 누락된 경우를 방어한다.
				continue; // 누락된 카테고리는 건너뛴다.
			}
			mappingsToInsert.add(MemberCategoryMapping.builder() // 새 매핑을 생성한다.
				.member(member) // 멤버를 연결한다.
				.category(category) // 카테고리를 연결한다.
				.relationType(key.relationType()) // 관계 타입을 설정한다.
				.build());
		}

		if (!mappingsToInsert.isEmpty()) { // 삽입 대상이 있는지 확인한다.
			memberCategoryMappingRepository.saveAll(mappingsToInsert); // 신규 매핑을 저장한다.
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

	// 요청 데이터로 비교 키 집합을 구성한다.
	private void addRequestedKeys(
		Set<MappingKey> requestedKeys,
		Map<Long, FoodCategory> requestedCategoriesById,
		List<FoodCategory> categories,
		MemberCategoryRelationType relationType
	) {
		for (FoodCategory category : categories) { // 요청 카테고리를 순회한다.
			MappingKey key = new MappingKey(category.getId(), relationType); // 카테고리+관계 타입 키를 만든다.
			requestedKeys.add(key); // 요청 키 집합에 추가한다.
			requestedCategoriesById.put(category.getId(), category); // 카테고리 ID로 엔티티를 저장한다.
		}
	}

	private record MappingKey(Long categoryId, MemberCategoryRelationType relationType) { // 비교용 키 레코드를 정의한다.
	}

	private record ValidationResult(
		List<FieldErrorData> errors,
		List<FoodCategory> categories
	) {
	}
}
