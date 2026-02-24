package com.matchimban.matchimban_api.auth.oauth.service;

import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.auth.jwt.RefreshTokenService;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthToken;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthUserInfo;
import com.matchimban.matchimban_api.auth.oauth.provider.OAuthProvider;
import com.matchimban.matchimban_api.auth.oauth.provider.OAuthProviderRegistry;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.member.entity.Member;
import java.net.URI;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrator 역할: OAuth 공통 흐름을 담당한다.
 * - state 발급/검증(1회성)
 * - provider(token/userinfo/unlink) 호출 순서 제어
 * - 회원/계정 연동 + JWT 쿠키 발급
 */
@Service
@Slf4j
public class OAuthService {

	private final OAuthProviderRegistry providerRegistry;
	private final OAuthStateService stateService;
	private final OAuthMemberService memberService;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;

	public OAuthService(
		OAuthProviderRegistry providerRegistry,
		OAuthStateService stateService,
		OAuthMemberService memberService,
		JwtTokenProvider jwtTokenProvider,
		RefreshTokenService refreshTokenService
	) {
		this.providerRegistry = providerRegistry;
		this.stateService = stateService;
		this.memberService = memberService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenService = refreshTokenService;
	}

	public URI startLogin(OAuthProviderType providerType) {
		OAuthProvider provider = providerRegistry.get(providerType);
		String state = stateService.issueState(providerType);
		return URI.create(provider.buildAuthorizeUrl(state));
	}

	public OAuthCallbackResult handleAuthCode(
		OAuthProviderType providerType,
		String error,
		String errorDescription,
		String state,
		String code
	) {
		if (error != null) {
			String detail = (errorDescription != null) ? errorDescription : error;
			throw new ApiException(AuthErrorCode.OAUTH_ACCESS_DENIED, detail);
		}
		if (!stateService.consumeState(providerType, state)) {
			throw new ApiException(AuthErrorCode.INVALID_OAUTH_STATE);
		}
		if (code == null || code.isBlank()) {
			throw new ApiException(AuthErrorCode.INVALID_REQUEST, "missing_code");
		}

		OAuthProvider provider = providerRegistry.get(providerType);
		OAuthToken token = provider.requestToken(code);
		OAuthUserInfo userInfo = provider.requestUserInfo(token.accessToken());
		Member member = memberService.findOrCreateMember(providerType, userInfo);

		String sid = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.createAccessToken(member, sid);
		String refreshToken = refreshTokenService.issue(member.getId(), sid, null);

		String redirectUrl = provider.frontendRedirectUrl();
		if (redirectUrl == null || redirectUrl.isBlank()) {
			redirectUrl = "/";
		}

		log.info("OAuth member linked. provider={}, memberId={}, status={}",
			providerType, member.getId(), member.getStatus());

		return new OAuthCallbackResult(
			member.getId(),
			member.getStatus().name(),
			redirectUrl,
			jwtTokenProvider.createAccessTokenCookie(accessToken),
			jwtTokenProvider.createRefreshTokenCookie(refreshToken)
		);
	}
}

