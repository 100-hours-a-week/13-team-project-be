package com.matchimban.matchimban_api.auth.dev;

import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.global.dto.ApiResult;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/dev/auth")
public class DevAuthController {
	private static final String PROVIDER_KAKAO = "KAKAO";
	private static final String DEV_PROVIDER_MEMBER_ID = "dev-kakao-0001";
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

	private Member getOrCreateDevMember() {
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

	private void ensureAgreements(Member member) {
		List<Policy> requiredPolicies = policyRepository.findByIsRequiredTrue();
		if (requiredPolicies.isEmpty()) {
			return;
		}

		LocalDateTime now = LocalDateTime.now();
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
