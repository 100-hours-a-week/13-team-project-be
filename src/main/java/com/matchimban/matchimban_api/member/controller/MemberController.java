package com.matchimban.matchimban_api.member.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.auth.jwt.JwtTokenProvider;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.global.swagger.MemberWithdrawErrorResponses;
import com.matchimban.matchimban_api.member.dto.response.MemberMeResponse;
import com.matchimban.matchimban_api.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member")
@Tag(name = "Member", description = "회원 정보 API")
public class MemberController {

	private final MemberService memberService;
	private final JwtTokenProvider jwtTokenProvider;

	public MemberController(
		MemberService memberService,
		JwtTokenProvider jwtTokenProvider
	) {
		this.memberService = memberService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@GetMapping("/me")
	@Operation(summary = "내 정보 조회", description = "로그인한 회원의 기본 프로필과 취향 정보를 조회한다.")
	@ApiResponse(responseCode = "200", description = "member_me_loaded")
	public ResponseEntity<ApiResult<MemberMeResponse>> getMyInfo() {
		Long memberId = requireMemberId();
		MemberMeResponse response = memberService.getMyInfo(memberId);
		return ResponseEntity.ok(ApiResult.of("member_me_loaded", response));
	}

	@DeleteMapping("/me")
	@CsrfRequired
	@Operation(summary = "회원 탈퇴", description = "회원 상태를 DELETED로 변경하고 카카오 연결을 해제한다.")
	@ApiResponse(responseCode = "200", description = "withdraw_success")
	@MemberWithdrawErrorResponses
	public ResponseEntity<ApiResult<?>> withdraw() {
		// 1) 인증된 사용자 확인
		Long memberId = requireMemberId();
		// 2) 내부 탈퇴 처리(soft delete) + refresh 전부 폐기 + 카카오 unlink
		memberService.withdraw(memberId);

		// 3) 브라우저 쿠키 만료(접근 토큰/리프레시 토큰)
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createExpiredAccessTokenCookie().toString());
		headers.add(HttpHeaders.SET_COOKIE, jwtTokenProvider.createExpiredRefreshTokenCookie().toString());

		// 4) 탈퇴 완료 응답
		return ResponseEntity.ok()
			.headers(headers)
			.body(ApiResult.of("withdraw_success"));
	}

	private Long requireMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
		}
		return principal.memberId();
	}
}
