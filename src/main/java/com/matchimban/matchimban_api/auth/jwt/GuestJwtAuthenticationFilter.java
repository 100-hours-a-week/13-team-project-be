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
public class GuestJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String QUICK_MEETINGS_PATH_PREFIX = "/api/v1/quick-meetings";

    private final GuestJwtTokenProvider guestJwtTokenProvider;
    private final JwtProperties jwtProperties;

    public GuestJwtAuthenticationFilter(GuestJwtTokenProvider guestJwtTokenProvider, JwtProperties jwtProperties) {
        this.guestJwtTokenProvider = guestJwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(QUICK_MEETINGS_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);
            if (token != null) {
                guestJwtTokenProvider.getAuthentication(token)
                        .ifPresent(auth -> setAuthentication(request, auth));
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
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (jwtProperties.guestCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}