package com.matchimban.matchimban_api.auth.jwt;

import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
import org.springframework.stereotype.Component;

@Component // DI를 위해서 붙임
public class JwtTokenProvider {

	private final JwtProperties properties;
	private SecretKey key;

	public JwtTokenProvider(JwtProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	private void initialize() {
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(Member member, String sid) { // 액세스 토큰 발급
		// 만료시간 설정
        Instant now = Instant.now();
		Instant expiresAt = now.plus(Duration.ofMinutes(properties.accessTokenExpireMinutes()));

		return Jwts.builder()
			.subject(String.valueOf(member.getId())) //member id.
			.issuer(properties.issuer())
			.issuedAt(Date.from(now))
			.expiration(Date.from(expiresAt))
			.claim("status", member.getStatus().name())
			// sid는 refresh 세션을 식별하기 위한 세션 키
			.claim("sid", sid)
			.signWith(key)
			.compact();
	}

	public ResponseCookie createAccessTokenCookie(String token) { //jwt를 쿠키로 만들기
		return ResponseCookie.from(properties.cookieName(), token)
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite(properties.cookieSameSite())
			.path("/")
			.maxAge(Duration.ofMinutes(properties.accessTokenExpireMinutes()))
			.build();
	}

	public ResponseCookie createRefreshTokenCookie(String token) {
		return ResponseCookie.from(properties.refreshCookieName(), token)
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite(properties.cookieSameSite())
			.path("/")
			.maxAge(Duration.ofDays(properties.refreshTokenExpireDays()))
			.build();
	}

	public ResponseCookie createExpiredAccessTokenCookie() {
		return ResponseCookie.from(properties.cookieName(), "")
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite(properties.cookieSameSite())
			.path("/")
			.maxAge(Duration.ZERO)
			.build();
	}

	public ResponseCookie createExpiredRefreshTokenCookie() {
		return ResponseCookie.from(properties.refreshCookieName(), "")
			.httpOnly(true)
			.secure(properties.cookieSecure())
			.sameSite(properties.cookieSameSite())
			.path("/")
			.maxAge(Duration.ZERO)
			.build();
	}

	public Optional<Authentication> getAuthentication(String token) { //토큰 → Authentication 만들기
		return parseToken(token, false)
			.map(principal -> new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	public Optional<MemberPrincipal> parsePrincipalAllowExpired(String token) {
		// refresh 요청 시 access token이 만료되어도 sid를 추출해야 한다.
		return parseToken(token, true);
	}

	private Optional<MemberPrincipal> parseToken(String token, boolean allowExpired) { //토큰 검증
		try {
			Claims claims = Jwts.parser()
				.requireIssuer(properties.issuer())
				.verifyWith(key) // 검증에 쓸 키 지정
				.build()
				.parseSignedClaims(token)// 서명 검증
				.getPayload();

			return toPrincipal(claims);
		} catch (ExpiredJwtException ex) {
			if (!allowExpired) {
				return Optional.empty();
			}
			return toPrincipal(ex.getClaims());
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	private Optional<MemberPrincipal> toPrincipal(Claims claims) {
		String subject = claims.getSubject();
		String statusValue = claims.get("status", String.class);
		String sid = claims.get("sid", String.class);
		if (subject == null || statusValue == null || sid == null) {
			return Optional.empty();
		}

		MemberStatus status = MemberStatus.valueOf(statusValue);
		return Optional.of(new MemberPrincipal(Long.valueOf(subject), status, sid));
	}

	// 설정 검증은 @ConfigurationProperties + @Validated에서 처리한다.
}
