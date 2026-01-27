package com.matchimban.matchimban_api.auth.kakao.service.serviceImpl;

import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;
import com.matchimban.matchimban_api.auth.kakao.service.KakaoMemberService;
import com.matchimban.matchimban_api.member.dto.MemberCreateRequest;
import com.matchimban.matchimban_api.member.dto.OAuthAccountCreateRequest;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.OAuthAccount;
import com.matchimban.matchimban_api.member.mapper.MemberMapper;
import com.matchimban.matchimban_api.member.mapper.OAuthAccountMapper;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import com.matchimban.matchimban_api.member.repository.OAuthAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoMemberServiceImpl implements KakaoMemberService {
	private static final String PROVIDER_KAKAO = "KAKAO";

	private final MemberRepository memberRepository;
	private final OAuthAccountRepository oauthAccountRepository;
	private final MemberMapper memberMapper;
	private final OAuthAccountMapper oauthAccountMapper;

	public KakaoMemberServiceImpl(
		MemberRepository memberRepository,
		OAuthAccountRepository oauthAccountRepository,
		MemberMapper memberMapper,
		OAuthAccountMapper oauthAccountMapper
	) {
		this.memberRepository = memberRepository;
		this.oauthAccountRepository = oauthAccountRepository;
		this.memberMapper = memberMapper;
		this.oauthAccountMapper = oauthAccountMapper;
	}

	@Override
	@Transactional
	public Member findOrCreateMember(KakaoUserInfo userInfo) {
		String providerMemberId = String.valueOf(userInfo.id());

		return oauthAccountRepository
			.findByProviderAndProviderMemberId(PROVIDER_KAKAO, providerMemberId)
			.map(OAuthAccount::getMember)
			.orElseGet(() -> createMemberWithAccount(userInfo, providerMemberId));
	}

	private Member createMemberWithAccount(KakaoUserInfo userInfo, String providerMemberId) {
		MemberCreateRequest memberRequest = new MemberCreateRequest(
			userInfo.nickname(),
			userInfo.profileImageUrl(),
			userInfo.thumbnailImageUrl()
		);
		Member member = memberMapper.toMember(memberRequest);
		memberRepository.save(member);

		OAuthAccountCreateRequest accountRequest = new OAuthAccountCreateRequest(
			PROVIDER_KAKAO,
			providerMemberId,
			member
		);
		OAuthAccount account = oauthAccountMapper.toOAuthAccount(accountRequest);
		oauthAccountRepository.save(account);
		return member;
	}
}
