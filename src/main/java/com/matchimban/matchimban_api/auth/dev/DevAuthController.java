package com.matchimban.matchimban_api.auth.dev;

import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.member.dto.MemberCreateRequest;
import com.matchimban.matchimban_api.member.dto.OAuthAccountCreateRequest;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.OAuthAccount;
import com.matchimban.matchimban_api.member.entity.MemberAgreement;
import com.matchimban.matchimban_api.member.entity.MemberCategoryMapping;
import com.matchimban.matchimban_api.member.entity.Policy;
import com.matchimban.matchimban_api.member.entity.FoodCategory;
import com.matchimban.matchimban_api.member.entity.enums.FoodCategoryType;
import com.matchimban.matchimban_api.member.entity.enums.MemberCategoryRelationType;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import com.matchimban.matchimban_api.member.mapper.MemberMapper;
import com.matchimban.matchimban_api.member.mapper.OAuthAccountMapper;
import com.matchimban.matchimban_api.member.repository.MemberAgreementRepository;
import com.matchimban.matchimban_api.member.repository.MemberCategoryMappingRepository;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import com.matchimban.matchimban_api.member.repository.OAuthAccountRepository;
import com.matchimban.matchimban_api.member.repository.FoodCategoryRepository;
import com.matchimban.matchimban_api.member.repository.PolicyRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/auth")
@Tag(name = "Dev Auth", description = "개발용 인증 유틸리티")
public class DevAuthController {
	private static final String PROVIDER_KAKAO = "KAKAO";
	private static final String DEV_PROVIDER_MEMBER_ID = "dev-kakao-0001";
	private static final String DEV_PROVIDER_MEMBER_ID_PREFIX = "dev-kakao-";
	private static final String DEV_NICKNAME = "DevUser";

	private final JwtTokenProvider jwtTokenProvider;
	private final MemberRepository memberRepository;
	private final OAuthAccountRepository oauthAccountRepository;
	private final MemberAgreementRepository memberAgreementRepository;
	private final PolicyRepository policyRepository;
	private final MemberCategoryMappingRepository memberCategoryMappingRepository;
	private final FoodCategoryRepository foodCategoryRepository;
	private final MemberMapper memberMapper;
	private final OAuthAccountMapper oauthAccountMapper;

	public DevAuthController(
		JwtTokenProvider jwtTokenProvider,
		MemberRepository memberRepository,
		OAuthAccountRepository oauthAccountRepository,
		MemberAgreementRepository memberAgreementRepository,
		PolicyRepository policyRepository,
		MemberCategoryMappingRepository memberCategoryMappingRepository,
		FoodCategoryRepository foodCategoryRepository,
		MemberMapper memberMapper,
		OAuthAccountMapper oauthAccountMapper
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.memberRepository = memberRepository;
		this.oauthAccountRepository = oauthAccountRepository;
		this.memberAgreementRepository = memberAgreementRepository;
		this.policyRepository = policyRepository;
		this.memberCategoryMappingRepository = memberCategoryMappingRepository;
		this.foodCategoryRepository = foodCategoryRepository;
		this.memberMapper = memberMapper;
		this.oauthAccountMapper = oauthAccountMapper;
	}

	@GetMapping("/access")
	@Transactional
	@Operation(summary = "개발용 액세스 토큰 발급", description = "dev 프로필에서 테스트 계정을 생성/조회하고 access 쿠키를 발급한다.")
	@ApiResponse(responseCode = "200", description = "dev_access_issued")
	public ResponseEntity<ApiResult<DevAccessData>> issueAccessToken() {
		Member member = getOrCreateDevMember();
		String sid = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.createAccessToken(member, sid);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(accessToken).toString());

		DevAccessData data = new DevAccessData(member.getId(), member.getNickname(), member.getStatus().name());
		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("dev_access_issued", data));
	}

	@GetMapping("/access/new")
	@Transactional
	@Operation(summary = "개발용 신규 계정 액세스 토큰 발급(항상 새로운 계정 생성)", description = "호출할 때마다 신규 테스트 계정을 생성하고 access 쿠키를 발급한다.")
	@ApiResponse(responseCode = "200", description = "dev_access_issued_new")
	public ResponseEntity<ApiResult<DevAccessData>> issueAccessTokenForNewMember() {
		// 호출할 때마다 항상 신규 개발 계정을 생성한다.
		Member member = createNewDevMember();
		String sid = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.createAccessToken(member, sid);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(accessToken).toString());

		DevAccessData data = new DevAccessData(member.getId(), member.getNickname(), member.getStatus().name());
		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("dev_access_issued_new", data));
	}

	@GetMapping("/access/member/{memberId}")
	@Transactional
	@Operation(summary = "(개발용) memberId으로 액세스 토큰 발급", description = "memberId가 개발용 계정인 경우에만 access 쿠키를 발급한다.")
	@ApiResponse(responseCode = "200", description = "dev_access_issued_by_member")
	@ApiResponse(responseCode = "403", description = "dev_account_required")
	public ResponseEntity<ApiResult<DevAccessData>> issueAccessTokenByMemberId(@PathVariable Long memberId) {
		// memberId로 토큰을 발급하는 기능은 개발 계정에만 허용.
		OAuthAccount account = oauthAccountRepository.findByMemberId(memberId)
			.orElseThrow(() -> new ApiException(AuthErrorCode.UNAUTHORIZED, "member_not_found"));

		if (!isDevAccount(account)) {
			throw new ApiException(AuthErrorCode.DEV_ACCOUNT_REQUIRED);
		}

		Member member = account.getMember();
		ensureAgreements(member);
		ensurePreferences(member);

		String sid = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.createAccessToken(member, sid);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(accessToken).toString());

		DevAccessData data = new DevAccessData(member.getId(), member.getNickname(), member.getStatus().name());
		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("dev_access_issued_by_member", data));
	}

	private Member getOrCreateDevMember() {
		// 고정 개발 계정(dev-kakao-0001)이 있으면 재사용.
		Optional<OAuthAccount> existing = oauthAccountRepository
			.findByProviderAndProviderMemberId(PROVIDER_KAKAO, DEV_PROVIDER_MEMBER_ID);
		if (existing.isPresent()) {
			Member member = existing.get().getMember();
			ensureAgreements(member);
			ensurePreferences(member);
			return member;
		}

		MemberCreateRequest memberRequest = new MemberCreateRequest(
			DEV_NICKNAME,
			null,
			null
		);
		Member member = memberMapper.toMember(memberRequest);
		member.updateStatus(MemberStatus.PENDING);
		memberRepository.save(member);

		OAuthAccountCreateRequest accountRequest = new OAuthAccountCreateRequest(
			PROVIDER_KAKAO,
			DEV_PROVIDER_MEMBER_ID,
			member
		);
		OAuthAccount account = oauthAccountMapper.toOAuthAccount(accountRequest);
		oauthAccountRepository.save(account);
		ensureAgreements(member);
		ensurePreferences(member);
		return member;
	}

	private Member createNewDevMember() {
		// dev-kakao-XXXX 형식의 순번 ID로 신규 개발 계정을 생성.
		String providerMemberId = nextDevProviderMemberId();
		String nickname = DEV_NICKNAME + "-" + providerMemberId.substring(DEV_PROVIDER_MEMBER_ID_PREFIX.length());

		MemberCreateRequest memberRequest = new MemberCreateRequest(
			nickname,
			null,
			null
		);
		Member member = memberMapper.toMember(memberRequest);
		member.updateStatus(MemberStatus.PENDING);
		memberRepository.save(member);

		OAuthAccountCreateRequest accountRequest = new OAuthAccountCreateRequest(
			PROVIDER_KAKAO,
			providerMemberId,
			member
		);
		OAuthAccount account = oauthAccountMapper.toOAuthAccount(accountRequest);
		oauthAccountRepository.save(account);
		ensureAgreements(member);
		ensurePreferences(member);
		return member;
	}

	private String nextDevProviderMemberId() {
		// 가장 큰 dev-kakao-XXXX를 찾아 +1 (0001은 고정 계정으로 예약).
		Optional<OAuthAccount> latest = oauthAccountRepository
			.findTopByProviderAndProviderMemberIdStartingWithOrderByProviderMemberIdDesc(PROVIDER_KAKAO, DEV_PROVIDER_MEMBER_ID_PREFIX);
		int nextSeq = 2; // reserve 0001 for the fixed dev account
		if (latest.isPresent()) {
			String providerMemberId = latest.get().getProviderMemberId();
			if (providerMemberId.startsWith(DEV_PROVIDER_MEMBER_ID_PREFIX)) {
				String suffix = providerMemberId.substring(DEV_PROVIDER_MEMBER_ID_PREFIX.length());
				try {
					nextSeq = Math.max(nextSeq, Integer.parseInt(suffix) + 1);
				} catch (NumberFormatException ignored) {
					nextSeq = 2;
				}
			}
		}
		return DEV_PROVIDER_MEMBER_ID_PREFIX + String.format("%04d", nextSeq);
	}

	private boolean isDevAccount(OAuthAccount account) {
		// 규칙 기반(dev-kakao- 접두어)으로 개발 계정 여부 판단.
		return PROVIDER_KAKAO.equalsIgnoreCase(account.getProvider())
			&& account.getProviderMemberId() != null
			&& account.getProviderMemberId().startsWith(DEV_PROVIDER_MEMBER_ID_PREFIX);
	}

	private void ensureAgreements(Member member) {
		// 개발 계정에 대해 필수 약관을 자동 동의 처리.
		List<Policy> requiredPolicies = policyRepository.findByIsRequiredTrue();
		if (requiredPolicies.isEmpty()) {
			return;
		}

		Instant now = Instant.now();
		List<MemberAgreement> toSave = new ArrayList<>();
		for (Policy policy : requiredPolicies) {
			boolean exists = memberAgreementRepository.existsByMemberIdAndPolicyId(member.getId(), policy.getId());
			if (exists) {
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
			member.updateStatus(MemberStatus.ONBOARDING);
			memberRepository.save(member);
		}
	}

	private void ensurePreferences(Member member) {
		// 기본 취향값 규칙:
		// - 알레르기: ALLERGY_GROUP의 첫 번째 항목
		// - 선호: CATEGORY의 첫 번째 항목
		// - 비선호: CATEGORY의 두 번째 항목(있고, 선호와 다를 때만)
		List<FoodCategory> allergyOptions = foodCategoryRepository.findByCategoryType(FoodCategoryType.ALLERGY_GROUP);
		List<FoodCategory> categoryOptions = foodCategoryRepository.findByCategoryType(FoodCategoryType.CATEGORY);

		FoodCategory allergy = allergyOptions.isEmpty() ? null : allergyOptions.get(0);
		FoodCategory preferred = categoryOptions.isEmpty() ? null : categoryOptions.get(0);
		FoodCategory disliked = categoryOptions.size() > 1 ? categoryOptions.get(1) : null;

		memberCategoryMappingRepository.deleteByMemberId(member.getId());
		memberCategoryMappingRepository.flush();

		List<MemberCategoryMapping> mappings = new ArrayList<>();
		if (allergy != null) {
			mappings.add(MemberCategoryMapping.builder()
				.member(member)
				.category(allergy)
				.relationType(MemberCategoryRelationType.ALLERGY)
				.build());
		}
		if (preferred != null) {
			mappings.add(MemberCategoryMapping.builder()
				.member(member)
				.category(preferred)
				.relationType(MemberCategoryRelationType.PREFERENCE)
				.build());
		}
		if (disliked != null && (preferred == null || !disliked.getId().equals(preferred.getId()))) {
			mappings.add(MemberCategoryMapping.builder()
				.member(member)
				.category(disliked)
				.relationType(MemberCategoryRelationType.DISLIKE)
				.build());
		}

		if (!mappings.isEmpty()) {
			memberCategoryMappingRepository.saveAll(mappings);
		}

		if (member.getStatus() != MemberStatus.ACTIVE) {
			member.updateStatus(MemberStatus.ACTIVE);
			memberRepository.save(member);
		}
	}

	private record DevAccessData(Long memberId, String nickname, String status) {
	}
}
