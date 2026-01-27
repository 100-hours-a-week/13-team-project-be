package com.matchimban.matchimban_api.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;

	public JwtAuthenticationFilter(
		JwtTokenProvider jwtTokenProvider,
		JwtProperties jwtProperties
	) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.jwtProperties = jwtProperties;
	}

	@Override
	protected void doFilterInternal( // 실제 필터 로직이 실행되는 지점
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = resolveToken(request);
			if (token != null) {
				jwtTokenProvider.getAuthentication(token).ifPresent(authentication -> {
					setAuthentication(request, authentication);
				});
			}
		}

		filterChain.doFilter(request, response);
	}

	private void setAuthentication(HttpServletRequest request, Authentication authentication) {
		if (authentication instanceof AbstractAuthenticationToken authToken) {
			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		}
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	private String resolveToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (jwtProperties.cookieName().equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
