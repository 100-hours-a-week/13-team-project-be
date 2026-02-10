package com.matchimban.matchimban_api.member.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.auth.jwt.RefreshTokenService;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import com.matchimban.matchimban_api.auth.oauth.provider.OAuthProviderRegistry;
import com.matchimban.matchimban_api.member.dto.response.MemberMeResponse;
import com.matchimban.matchimban_api.member.dto.response.MemberPreferencesResponse;
import com.matchimban.matchimban_api.member.dto.response.PreferenceItemResponse;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.MemberCategoryMapping;
import com.matchimban.matchimban_api.member.entity.OAuthAccount;
import com.matchimban.matchimban_api.member.entity.enums.MemberCategoryRelationType;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import com.matchimban.matchimban_api.member.error.MemberErrorCode;
import com.matchimban.matchimban_api.member.repository.MemberCategoryMappingRepository;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import com.matchimban.matchimban_api.member.repository.OAuthAccountRepository;
import com.matchimban.matchimban_api.member.service.MemberService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl implements MemberService {
	private final MemberRepository memberRepository;
	private final MemberCategoryMappingRepository memberCategoryMappingRepository;
	private final OAuthAccountRepository oauthAccountRepository;
	private final OAuthProviderRegistry oauthProviderRegistry;
	private final RefreshTokenService refreshTokenService;

	public MemberServiceImpl(
		MemberRepository memberRepository,
		MemberCategoryMappingRepository memberCategoryMappingRepository,
		OAuthAccountRepository oauthAccountRepository,
		OAuthProviderRegistry oauthProviderRegistry,
		RefreshTokenService refreshTokenService
	) {
		this.memberRepository = memberRepository;
		this.memberCategoryMappingRepository = memberCategoryMappingRepository;
		this.oauthAccountRepository = oauthAccountRepository;
		this.oauthProviderRegistry = oauthProviderRegistry;
		this.refreshTokenService = refreshTokenService;
	}

	@Override
	@Transactional(readOnly = true)
	public MemberMeResponse getMyInfo(Long memberId) {
		// 인증된 사용자 기준으로 회원 정보를 조회한다.
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ApiException(MemberErrorCode.UNAUTHORIZED));

		// 취향/알레르기 매핑을 카테고리와 함께 조회한다.
		List<MemberCategoryMapping> mappings = memberCategoryMappingRepository
			.findByMemberIdWithCategory(memberId);

		// 관계 타입별로 응답 리스트를 분리해 담는다.
		Map<MemberCategoryRelationType, List<PreferenceItemResponse>> grouped = new EnumMap<>(MemberCategoryRelationType.class);
		for (MemberCategoryRelationType type : MemberCategoryRelationType.values()) {
			grouped.put(type, new ArrayList<>());
		}
		for (MemberCategoryMapping mapping : mappings) {
			// 카테고리 엔티티를 응답용 DTO로 변환한다.
			grouped.get(mapping.getRelationType()).add(new PreferenceItemResponse(
				mapping.getCategory().getCategoryCode(),
				mapping.getCategory().getCategoryName(),
				mapping.getCategory().getEmoji()
			));
		}

		// 프론트에서 바로 쓰기 좋게 세 그룹으로 묶어 내려준다.
		MemberPreferencesResponse preferences = new MemberPreferencesResponse(
			grouped.get(MemberCategoryRelationType.ALLERGY),
			grouped.get(MemberCategoryRelationType.PREFERENCE),
			grouped.get(MemberCategoryRelationType.DISLIKE)
		);

		// 기본 프로필 정보 + 취향 정보를 함께 반환한다.
		return new MemberMeResponse(
			member.getId(),
			member.getNickname(),
			member.getProfileImageUrl(),
			member.getThumbnailImageUrl(),
			member.getStatus().name(),
			member.getCreatedAt(),
			member.getUpdatedAt(),
			preferences
		);
	}

	@Override
	@Transactional
	public void withdraw(Long memberId) {
		// 1) 회원 존재 및 인증 확인
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ApiException(MemberErrorCode.UNAUTHORIZED));

		// 2) 이미 탈퇴 상태면 추가 처리 없이 종료
		if (member.getStatus() == MemberStatus.DELETED) {
			throw new ApiException(MemberErrorCode.ALREADY_WITHDRAWN);
		}

		// 3) 내부 계정 soft delete 처리
		member.updateStatus(MemberStatus.DELETED);
		// 4) 모든 refresh 세션 폐기 (모든 기기 강제 로그아웃)
		refreshTokenService.revokeAll(memberId);

		// 5) 외부 OAuth Provider 연결 해제 (provider에 따라 unlink)
		OAuthAccount account = oauthAccountRepository.findByMemberId(memberId).orElse(null);
		if (account != null) {
			try {
				OAuthProviderType providerType = OAuthProviderType.fromProvider(account.getProvider());
				oauthProviderRegistry.find(providerType)
					.ifPresent(provider -> provider.unlink(account.getProviderMemberId()));
			} catch (IllegalArgumentException ex) {
				// 알 수 없는 provider면 외부 unlink는 스킵하고, 내부 연동 정보만 정리한다.
			}
			// 재로그인 시 새 계정 생성되도록 연결 계정도 삭제
			oauthAccountRepository.delete(account);
		}
	}
}
