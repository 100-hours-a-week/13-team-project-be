package com.matchimban.matchimban_api.auth.controller;

import com.matchimban.matchimban_api.auth.jwt.JwtProperties;
import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.auth.jwt.RefreshTokenService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import com.matchimban.matchimban_api.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
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
	public ResponseEntity<ApiResult<?>> refresh(HttpServletRequest request) {
		// refresh는 access(만료 허용) + refresh 쿠키가 둘 다 있어야 한다.
		String accessToken = resolveCookie(request, jwtProperties.cookieName());
		String refreshToken = resolveCookie(request, jwtProperties.refreshCookieName());
		if (accessToken == null || refreshToken == null) {
			return unauthorizedWithClearCookies("invalid_refresh_token");
		}

		// 만료된 access라도 sid를 얻기 위해 파싱을 허용한다.
		Optional<MemberPrincipal> principalOpt = jwtTokenProvider.parsePrincipalAllowExpired(accessToken);
		if (principalOpt.isEmpty()) {
			return unauthorizedWithClearCookies("invalid_access_token");
		}
		MemberPrincipal principal = principalOpt.get();

		Member member = memberRepository.findById(principal.memberId())
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized"));
		if (member.getStatus() == MemberStatus.DELETED) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiResult.of("account_deleted"));
		}

		// Redis에서 refresh 해시 검증 → 성공 시 새 refresh 발급
		Optional<String> rotated = refreshTokenService.rotate(
			member.getId(),
			principal.sid(),
			refreshToken,
			request.getHeader("User-Agent")
		);
		if (rotated.isEmpty()) {
			return unauthorizedWithClearCookies("invalid_refresh_token");
		}

		// 같은 sid로 access/refresh를 교체해 세션을 유지한다.
		String newAccessToken = jwtTokenProvider.createAccessToken(member, principal.sid());
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createAccessTokenCookie(newAccessToken).toString());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createRefreshTokenCookie(rotated.get()).toString());

		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("token_refreshed"));
	}

	@PostMapping("/logout")
	@CsrfRequired
	public ResponseEntity<ApiResult<?>> logout(HttpServletRequest request) {
		String accessToken = resolveCookie(request, jwtProperties.cookieName());
		if (accessToken != null) {
			// 로그아웃은 sid 세션 단위로 refresh를 삭제한다.
			jwtTokenProvider.parsePrincipalAllowExpired(accessToken)
				.ifPresent(principal -> refreshTokenService.revoke(principal.memberId(), principal.sid()));
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
