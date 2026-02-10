package com.matchimban.matchimban_api.auth.oauth.service;

import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthUserInfo;
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
public class OAuthMemberService {

	private final MemberRepository memberRepository;
	private final OAuthAccountRepository oauthAccountRepository;
	private final MemberMapper memberMapper;
	private final OAuthAccountMapper oauthAccountMapper;

	public OAuthMemberService(
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

	@Transactional
	public Member findOrCreateMember(OAuthProviderType providerType, OAuthUserInfo userInfo) {
		return oauthAccountRepository
			.findByProviderAndProviderMemberId(providerType.provider(), userInfo.providerMemberId())
			.map(OAuthAccount::getMember)
			.orElseGet(() -> createMemberWithAccount(providerType, userInfo));
	}

	private Member createMemberWithAccount(OAuthProviderType providerType, OAuthUserInfo userInfo) {
		MemberCreateRequest memberRequest = new MemberCreateRequest(
			userInfo.nickname(),
			userInfo.profileImageUrl(),
			userInfo.thumbnailImageUrl()
		);
		Member member = memberMapper.toMember(memberRequest);
		memberRepository.save(member);

		OAuthAccountCreateRequest accountRequest = new OAuthAccountCreateRequest(
			providerType.provider(),
			userInfo.providerMemberId(),
			member
		);
		OAuthAccount account = oauthAccountMapper.toOAuthAccount(accountRequest);
		oauthAccountRepository.save(account);
		return member;
	}
}

