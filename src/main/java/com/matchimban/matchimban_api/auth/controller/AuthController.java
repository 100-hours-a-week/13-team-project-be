package com.matchimban.matchimban_api.auth.controller;

import com.matchimban.matchimban_api.auth.jwt.JwtProperties;
import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.auth.jwt.RefreshTokenService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.global.swagger.AuthRefreshErrorResponses;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 토큰 관리 API")
public class AuthController {

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final RefreshTokenService refreshTokenService;
	private final MemberRepository memberRepository;

	public AuthController(
		JwtTokenProvider jwtTokenProvider,
		JwtProperties jwtProperties,
		RefreshTokenService refreshTokenService,
		MemberRepository memberRepository
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtProperties = jwtProperties;
		this.refreshTokenService = refreshTokenService;
		this.memberRepository = memberRepository;
	}

	@PostMapping("/refresh")
	@CsrfRequired
	@Operation(summary = "토큰 갱신", description = "리프레시 토큰을 검증하고 AT/RT를 교체한다.")
	@ApiResponse(responseCode = "200", description = "token_refreshed")
	@AuthRefreshErrorResponses
	public ResponseEntity<ApiResult<?>> refresh(HttpServletRequest request) {
		// refresh는 refresh 쿠키만 있으면 갱신할 수 있다.
		String refreshToken = resolveCookie(request, jwtProperties.refreshCookieName());
		if (refreshToken == null) {
			return unauthorizedWithClearCookies("invalid_refresh_token");
		}

		// refresh 토큰으로 세션(memberId/sid)을 역조회한다.
		Optional<RefreshTokenService.RefreshSession> sessionOpt = refreshTokenService.resolveSession(refreshToken);
		if (sessionOpt.isEmpty()) {
			return unauthorizedWithClearCookies("invalid_refresh_token");
		}
		RefreshTokenService.RefreshSession session = sessionOpt.get();

		Member member = memberRepository.findById(session.memberId())
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized"));
		if (member.getStatus() == MemberStatus.DELETED) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResult.of("account_deleted"));
		}

		// Redis에서 refresh 해시 검증 → 성공 시 새 refresh 발급
		Optional<String> rotated = refreshTokenService.rotate(
			session.memberId(),
			session.sid(),
			refreshToken,
			request.getHeader("User-Agent")
		);
		if (rotated.isEmpty()) {
			return unauthorizedWithClearCookies("invalid_refresh_token");
		}

		// 같은 sid로 access/refresh를 교체해 세션을 유지한다.
		String newAccessToken = jwtTokenProvider.createAccessToken(member, session.sid());
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(newAccessToken).toString());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createRefreshTokenCookie(rotated.get()).toString());

		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("token_refreshed"));
	}

	@PostMapping("/logout")
	@CsrfRequired
	@Operation(summary = "로그아웃", description = "현재 세션의 리프레시 토큰을 폐기하고 쿠키를 만료한다.")
	@ApiResponse(responseCode = "200", description = "logout_success")
	public ResponseEntity<ApiResult<?>> logout(HttpServletRequest request) {
		// access 없이도 refresh 기반으로 세션을 폐기한다.
		String refreshToken = resolveCookie(request, jwtProperties.refreshCookieName());
		if (refreshToken != null) {
			refreshTokenService.resolveSession(refreshToken)
				.ifPresent(session -> refreshTokenService.revoke(session.memberId(), session.sid()));
		}

		// 쿠키 만료 처리 (브라우저 세션 종료)
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createExpiredAccessTokenCookie().toString());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createExpiredRefreshTokenCookie().toString());

		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("logout_success"));
	}

	private String resolveCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private ResponseEntity<ApiResult<?>> unauthorizedWithClearCookies(String message) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createExpiredAccessTokenCookie().toString());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createExpiredRefreshTokenCookie().toString());
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.headers(headers)
			.body(ApiResult.of(message));
	}
}
