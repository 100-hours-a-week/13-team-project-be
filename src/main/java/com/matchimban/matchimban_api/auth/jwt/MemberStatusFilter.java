package com.matchimban.matchimban_api.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.global.error.ErrorResponse;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MemberStatusFilter extends OncePerRequestFilter {

	private static final String ONBOARDING_BASE_PATH = "/api/v1/onboarding";
	private static final String AGREEMENTS_PATH = ONBOARDING_BASE_PATH + "/agreements";
	private static final String PREFERENCES_PATH = ONBOARDING_BASE_PATH + "/preferences";
	private static final String USER_ME_PATH = "/api/v1/member/me";

	private final ObjectMapper objectMapper;

	public MemberStatusFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return path.startsWith("/api/v1/auth")
			|| path.startsWith("/swagger-ui")
			|| path.startsWith("/v3/api-docs")
			|| path.equals("/swagger-ui.html")
			|| path.equals("/api/ping")
			|| path.equals("/api/csrf")
			|| path.equals("/actuator/health");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
			filterChain.doFilter(request, response);
			return;
		}

		MemberStatus status = principal.status();
		String path = request.getRequestURI();

		if (status == MemberStatus.DELETED) {
			writeError(response, HttpStatus.FORBIDDEN, "account_deleted");
			return;
		}

		if (status == MemberStatus.PENDING && !isPendingAllowed(path)) {
			writeError(response, HttpStatus.FORBIDDEN, "agreement_required");
			return;
		}

		if (status == MemberStatus.ONBOARDING && !isOnboardingAllowed(path)) {
			writeError(response, HttpStatus.FORBIDDEN, "preferences_required");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean isPendingAllowed(String path) {
		return path.startsWith(AGREEMENTS_PATH) || path.equals(USER_ME_PATH);
	}

	private boolean isOnboardingAllowed(String path) {
		return path.startsWith(PREFERENCES_PATH) || path.equals(USER_ME_PATH);
	}

	private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(message));
	}
}
