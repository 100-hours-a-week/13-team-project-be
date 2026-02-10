package com.matchimban.matchimban_api.auth.kakao.controller;

import com.matchimban.matchimban_api.auth.kakao.dto.KakaoAuthCodeRequest;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoAuthCallbackResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoLoginResponse;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import com.matchimban.matchimban_api.auth.oauth.service.OAuthCallbackResult;
import com.matchimban.matchimban_api.auth.oauth.service.OAuthService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.swagger.CommonAuthErrorResponses;
import com.matchimban.matchimban_api.global.swagger.InternalServerErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Auth", description = "Kakao OAuth endpoints")
@RestController
@Slf4j
@RequestMapping("/api/v1/auth/kakao")
public class KakaoAuthController {
    private final OAuthService oauthService;

    public KakaoAuthController(
            OAuthService oauthService
    ) {
        this.oauthService = oauthService;
    }


	@Operation(summary = "카카오 로그인 시작", description = "카카오 인가 페이지로 302 Redirect")
	@ApiResponse(responseCode = "302", description = "redirect_to_kakao_authorize")
	@InternalServerErrorResponse
	@GetMapping("/login")
	public ResponseEntity<ApiResult<KakaoLoginResponse>> login() {
		URI authorizeUri = oauthService.startLogin(OAuthProviderType.KAKAO);

		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(authorizeUri);
		KakaoLoginResponse data = new KakaoLoginResponse(authorizeUri.toString());
		return new ResponseEntity<>(ApiResult.of("redirect_to_kakao_authorize", data), headers, HttpStatus.FOUND);
	}


	@Operation(summary = "카카오 인가코드 콜백", description = "state 검증 후 302 Redirect")
	@ApiResponse(responseCode = "302", description = "login_success_redirect_to_...")
	@CommonAuthErrorResponses
	@GetMapping("/auth-code")
	public ResponseEntity<ApiResult<KakaoAuthCallbackResponse>> authCode(@ParameterObject KakaoAuthCodeRequest request) {
		OAuthCallbackResult result = oauthService.handleAuthCode(
			OAuthProviderType.KAKAO,
			request.error(),
			request.errorDescription(),
			request.state(),
			request.code()
		);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, result.accessTokenCookie().toString());
		headers.add(HttpHeaders.SET_COOKIE, result.refreshTokenCookie().toString());
		headers.setLocation(URI.create(result.redirectUrl()));

		KakaoAuthCallbackResponse data = new KakaoAuthCallbackResponse(result.redirectUrl(), result.memberStatus());
		log.info("Member linked. id={}, status={}", result.memberId(), result.memberStatus());
		return new ResponseEntity<>(ApiResult.of("login_success_redirect", data), headers, HttpStatus.FOUND);
	}
}
