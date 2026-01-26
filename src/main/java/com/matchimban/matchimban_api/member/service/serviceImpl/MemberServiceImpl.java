package com.matchimban.matchimban_api.member.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.member.dto.response.MemberMeResponse;
import com.matchimban.matchimban_api.member.dto.response.MemberPreferencesResponse;
import com.matchimban.matchimban_api.member.dto.response.PreferenceItemResponse;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.MemberCategoryMapping;
import com.matchimban.matchimban_api.member.entity.enums.MemberCategoryRelationType;
import com.matchimban.matchimban_api.member.repository.MemberCategoryMappingRepository;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
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

	public MemberServiceImpl(
		MemberRepository memberRepository,
		MemberCategoryMappingRepository memberCategoryMappingRepository
	) {
		this.memberRepository = memberRepository;
		this.memberCategoryMappingRepository = memberCategoryMappingRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public MemberMeResponse getMyInfo(Long memberId) {
		// 인증된 사용자 기준으로 회원 정보를 조회한다.
		Member member = memberRepository.findById(memberId)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized"));

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
}
