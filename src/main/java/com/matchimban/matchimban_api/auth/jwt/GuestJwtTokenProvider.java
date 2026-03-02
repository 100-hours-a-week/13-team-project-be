package com.matchimban.matchimban_api.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class GuestJwtTokenProvider {

    private static final String TOKEN_TYPE_GUEST = "GUEST";
    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String CLAIM_MEETING_ID = "mid";
    private static final String GUEST_COOKIE_PATH = "/api/v1/quick-meetings";

    private final JwtProperties properties;
    private SecretKey key;

    public GuestJwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    private void initialize() {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public record GuestTokenIssueResult(String token, Instant expiresAt) {}

    public GuestTokenIssueResult issueGuestAccessToken(Long guestMemberId, Long meetingId, Instant requestedExpiresAt) {
        Instant now = Instant.now();
        Instant cap = now.plus(Duration.ofMinutes(properties.guestAccessTokenMaxMinutes()));
        Instant expiresAt = requestedExpiresAt.isAfter(cap) ? cap : requestedExpiresAt;
        if (!expiresAt.isAfter(now)) expiresAt = now.plusSeconds(1);

        String token = Jwts.builder()
                .subject(String.valueOf(guestMemberId))
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_GUEST)
                .claim(CLAIM_MEETING_ID, String.valueOf(meetingId))
                .signWith(key)
                .compact();

        return new GuestTokenIssueResult(token, expiresAt);
    }

    public ResponseCookie createGuestAccessTokenCookie(String token, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        if (maxAge.isNegative()) maxAge = Duration.ZERO;

        return ResponseCookie.from(properties.guestCookieName(), token)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(GUEST_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie createExpiredGuestAccessTokenCookie() {
        return ResponseCookie.from(properties.guestCookieName(), "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(GUEST_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    public Optional<Authentication> getAuthentication(String token) {
        return parseToken(token)
                .map(principal -> new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_GUEST"))
                ));
    }

    private Optional<GuestPrincipal> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .requireIssuer(properties.issuer())
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return toPrincipal(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Optional<GuestPrincipal> toPrincipal(Claims claims) {
        String subject = claims.getSubject();
        String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
        String mid = claims.get(CLAIM_MEETING_ID, String.class);

        if (subject == null || typ == null || mid == null) return Optional.empty();
        if (!TOKEN_TYPE_GUEST.equals(typ)) return Optional.empty();

        try {
            return Optional.of(new GuestPrincipal(Long.valueOf(subject), Long.valueOf(mid)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}