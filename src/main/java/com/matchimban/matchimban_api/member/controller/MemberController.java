package com.matchimban.matchimban_api.member.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.member.dto.response.MemberMeResponse;
import com.matchimban.matchimban_api.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResult<MemberMeResponse>> getMyInfo() {
		Long memberId = requireMemberId();
		MemberMeResponse response = memberService.getMyInfo(memberId);
		return ResponseEntity.ok(ApiResult.of("member_me_loaded", response));
	}

	private Long requireMemberId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
		}
		return principal.memberId();
	}
}
