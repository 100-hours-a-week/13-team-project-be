package com.matchimban.matchimban_api.auth.kakao.controller;

import com.matchimban.matchimban_api.auth.kakao.config.KakaoOAuthProperties;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoAuthCodeRequest;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoAuthCallbackResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoLoginResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoTokenResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;
import com.matchimban.matchimban_api.auth.kakao.service.KakaoAuthService;
import com.matchimban.matchimban_api.auth.kakao.service.KakaoMemberService;
import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.auth.jwt.RefreshTokenService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.global.swagger.CommonAuthErrorResponses;
import com.matchimban.matchimban_api.global.swagger.InternalServerErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Kakao OAuth endpoints")
@RestController
@Slf4j
@RequestMapping("/api/v1/auth/kakao")
public class KakaoAuthController {

	private final KakaoAuthService kakaoAuthService;
	private final KakaoMemberService kakaoMemberService;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final KakaoOAuthProperties kakaoOAuthProperties;

	public KakaoAuthController(
		KakaoAuthService kakaoAuthService,
		KakaoMemberService kakaoMemberService,
		JwtTokenProvider jwtTokenProvider,
		RefreshTokenService refreshTokenService,
		KakaoOAuthProperties kakaoOAuthProperties
	) {
		this.kakaoAuthService = kakaoAuthService;
		this.kakaoMemberService = kakaoMemberService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.refreshTokenService = refreshTokenService;
		this.kakaoOAuthProperties = kakaoOAuthProperties;
	}

	@Operation(summary = "카카오 로그인 시작", description = "카카오 인가 페이지로 302 Redirect")
	@ApiResponse(responseCode = "302", description = "redirect_to_kakao_authorize")
	@InternalServerErrorResponse
	@GetMapping("/login")
	public ResponseEntity<ApiResult<KakaoLoginResponse>> login() {
		String state = kakaoAuthService.issueState();
		String loginUrl = kakaoAuthService.buildAuthorizeUrl(state);

		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create(loginUrl));
		KakaoLoginResponse data = new KakaoLoginResponse(loginUrl);
		return new ResponseEntity<>(ApiResult.of("redirect_to_kakao_authorize", data), headers, HttpStatus.FOUND);
	}


	@Operation(summary = "카카오 인가코드 콜백", description = "state 검증 후 302 Redirect")
	@ApiResponse(responseCode = "302", description = "login_success_redirect_to_...")
	@CommonAuthErrorResponses
	@GetMapping("/auth-code")
	public ResponseEntity<ApiResult<KakaoAuthCallbackResponse>> authCode(@ParameterObject KakaoAuthCodeRequest request) {
		String error = request.error();
		String errorDescription = request.errorDescription();
		String state = request.state();
		String code = request.code();

		if (error != null) {
			String detail = errorDescription != null ? errorDescription : error;
			throw new ApiException(HttpStatus.FORBIDDEN, "oauth_access_denied", detail);
		}
		if (!kakaoAuthService.consumeState(state)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_oauth_state");
		}
		if (code == null || code.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request");
		}

		KakaoTokenResponse tokenResponse = kakaoAuthService.requestToken(code);
		KakaoUserInfo userInfo = kakaoAuthService.requestUserInfo(tokenResponse.accessToken());
		Member member = kakaoMemberService.findOrCreateMember(userInfo);

		HttpHeaders headers = new HttpHeaders();
		String sid = UUID.randomUUID().toString();
		String accessToken = jwtTokenProvider.createAccessToken(member, sid);
		String refreshToken = refreshTokenService.issue(member.getId(), sid, null);
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(accessToken).toString());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createRefreshTokenCookie(refreshToken).toString());
		String redirectUrl = kakaoOAuthProperties.frontendRedirectUrl();
		if (redirectUrl == null || redirectUrl.isBlank()) {
			redirectUrl = "/";
		}
		headers.setLocation(URI.create(redirectUrl));

		KakaoAuthCallbackResponse data = new KakaoAuthCallbackResponse(redirectUrl, member.getStatus().name());
		log.info("Member linked. id={}, status={}", member.getId(), member.getStatus());
		return new ResponseEntity<>(ApiResult.of("login_success_redirect", data), headers, HttpStatus.FOUND);
	}
}
