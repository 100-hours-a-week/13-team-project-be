package com.matchimban.matchimban_api.member.onboarding.config;

import com.matchimban.matchimban_api.member.entity.FoodCategory;
import com.matchimban.matchimban_api.member.entity.Policy;
import com.matchimban.matchimban_api.member.entity.enums.FoodCategoryType;
import com.matchimban.matchimban_api.member.entity.enums.PolicyType;
import com.matchimban.matchimban_api.member.repository.FoodCategoryRepository;
import com.matchimban.matchimban_api.member.repository.PolicyRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class OnboardingSeedData implements ApplicationRunner { //ApplicationRunner로 서버 시작시 1회실행

	private static final String TERMS_OF_SERVICE_CONTENT = """
		제1조 (목적)
		이 약관은 Matchimban(이하 "회사")가 제공하는 모임 매칭 서비스(이하 "서비스")의 이용과 관련하여 회사와 회원 간의 권리, 의무 및 책임사항을 규정합니다.
		제2조 (정의)
		1. "회원"이란 카카오 계정을 통해 가입하고 본 약관에 동의한 자를 말합니다.
		2. "콘텐츠"란 회원이 서비스에 게시하거나 제공하는 정보(텍스트, 이미지 등)를 말합니다.
		제3조 (회원가입 및 계정 관리)
		1. 회원은 정확한 정보를 제공해야 하며, 변경 시 즉시 수정해야 합니다.
		2. 회사는 허위 정보, 타인 명의 도용 등 부정한 가입을 제한할 수 있습니다.
		제4조 (서비스 제공 및 변경)
		1. 회사는 서비스 품질 향상을 위해 일부 기능을 변경 또는 중단할 수 있습니다.
		2. 서비스 변경 시 회사는 사전에 공지합니다. 다만 긴급한 경우 사후 공지할 수 있습니다.
		제5조 (회원의 의무)
		1. 회원은 법령 및 본 약관을 준수해야 합니다.
		2. 다음 행위를 금지합니다: 타인 권리 침해, 불법 콘텐츠 게시, 서비스 운영 방해.
		제6조 (이용 제한 및 해지)
		1. 회사는 약관 위반 시 서비스 이용을 제한할 수 있습니다.
		2. 회원은 언제든지 탈퇴할 수 있으며, 탈퇴 시 관련 법령에 따른 보관 정보를 제외하고 삭제됩니다.
		제7조 (면책)
		회사는 천재지변, 시스템 장애 등 불가항력으로 인한 서비스 제공 중단에 대해 책임을 지지 않습니다.
		""";

	private static final String PRIVACY_POLICY_CONTENT = """
		1. 수집 항목
		- 필수: 카카오 식별자, 닉네임, 프로필 이미지, 접속 로그
		- 선택: 취향/알레르기 정보
		2. 수집 목적
		- 회원 식별, 맞춤형 모임 추천, 서비스 운영/개선
		3. 보유 및 이용기간
		- 회원 탈퇴 시 지체 없이 파기, 단 관련 법령에 따라 보관이 필요한 정보는 법정기간 보관
		4. 제3자 제공
		- 원칙적으로 제공하지 않음. 법령에 따른 요청 시 제공될 수 있음.
		5. 처리 위탁
		- 서비스 운영에 필요한 범위에서 외부 업체에 위탁할 수 있으며, 변경 시 공지함.
		6. 이용자의 권리
		- 열람, 정정, 삭제, 처리 정지를 요청할 수 있음.
		7. 안전성 확보 조치
		- 접근 통제, 암호화, 로그 관리 등 합리적 보호조치를 시행함.
		8. 문의처
		- 문의: support@matchimban.com
		""";

	private final PolicyRepository policyRepository;
	private final FoodCategoryRepository foodCategoryRepository;

	public OnboardingSeedData(
		PolicyRepository policyRepository,
		FoodCategoryRepository foodCategoryRepository
	) {
		this.policyRepository = policyRepository;
		this.foodCategoryRepository = foodCategoryRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		seedPolicies();
		seedFoodCategories();
	}

	private void seedPolicies() {
		List<PolicySeed> seeds = List.of(
			new PolicySeed(
				PolicyType.TERMS_OF_SERVICE,
				"이용약관",
				"1.0",
				true,
				"서비스 이용 및 회원 관리\n계정 운영 및 제재 기준",
				TERMS_OF_SERVICE_CONTENT
			),
			new PolicySeed(
				PolicyType.PRIVACY_POLICY,
				"개인정보처리방침",
				"1.0",
				true,
				"개인정보 수집 및 이용\n보유·파기 기준\n이용자 권리 안내",
				PRIVACY_POLICY_CONTENT
			)
		);

		for (PolicySeed seed : seeds) {
			boolean exists = policyRepository
				.findByPolicyTypeAndTermsVersion(seed.policyType(), seed.version())
				.isPresent();
			if (exists) {
				continue;
			}

			Policy policy = Policy.builder()
				.policyType(seed.policyType())
				.title(seed.title())
				.termsVersion(seed.version())
				.isRequired(seed.required())
				.summary(seed.summary())
				.termsContent(seed.content())
				.createdAt(LocalDateTime.now())
				.build();
			try {
				policyRepository.save(policy);
			} catch (DataIntegrityViolationException ignored) {
				// 다른 인스턴스가 넣은 상황
			}
		}
	}

	private void seedFoodCategories() {
		List<FoodCategorySeed> seeds = List.of(
			new FoodCategorySeed(FoodCategoryType.ALLERGY_GROUP, "NUTS", "견과류", "🥜"),
			new FoodCategorySeed(FoodCategoryType.ALLERGY_GROUP, "DAIRY", "유제품", "🥛"),
			new FoodCategorySeed(FoodCategoryType.ALLERGY_GROUP, "SEAFOOD", "해산물", "🦐"),
			new FoodCategorySeed(FoodCategoryType.ALLERGY_GROUP, "GRAIN_GLUTEN", "곡류/글루텐", "🌾"),
			new FoodCategorySeed(FoodCategoryType.ALLERGY_GROUP, "MEAT", "육류", "🍖"),
			new FoodCategorySeed(FoodCategoryType.ALLERGY_GROUP, "FRUIT_ETC", "과일/기타", "🍑"),
			new FoodCategorySeed(FoodCategoryType.CATEGORY, "KOREAN", "한식", null),
			new FoodCategorySeed(FoodCategoryType.CATEGORY, "CHINESE", "중식", null),
			new FoodCategorySeed(FoodCategoryType.CATEGORY, "JAPANESE", "일식", null),
			new FoodCategorySeed(FoodCategoryType.CATEGORY, "WESTERN", "양식", null),
			new FoodCategorySeed(FoodCategoryType.CATEGORY, "SEAFOOD", "해산물", null),
			new FoodCategorySeed(FoodCategoryType.CATEGORY, "MEAT", "고기", null)
		);

		for (FoodCategorySeed seed : seeds) {
			boolean exists = foodCategoryRepository
				.findByCategoryTypeAndCategoryCode(seed.categoryType(), seed.code())
				.isPresent();
			if (exists) {
				continue;
			}

			FoodCategory category = FoodCategory.builder()
				.categoryType(seed.categoryType())
				.categoryCode(seed.code())
				.categoryName(seed.label())
				.emoji(seed.emoji())
				.build();
			try {
				foodCategoryRepository.save(category);
			} catch (DataIntegrityViolationException ignored) {
				// Ignore duplicates on multi-instance startup.
			}
		}
	}

	private record PolicySeed(
		PolicyType policyType,
		String title,
		String version,
		boolean required,
		String summary,
		String content
	) {
	}

	private record FoodCategorySeed(
		FoodCategoryType categoryType,
		String code,
		String label,
		String emoji
	) {
	}
}
