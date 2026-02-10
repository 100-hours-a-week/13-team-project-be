package com.matchimban.matchimban_api.auth.kakao.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.kakao.config.KakaoOAuthProperties;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthToken;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthUserInfo;
import com.matchimban.matchimban_api.auth.oauth.provider.OAuthProvider;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Strategy 역할: "Kakao OAuth"에 대한 provider-specific 로직만 담당한다.
 * - 공통 흐름(state 검증, 회원 생성/연동, JWT 발급)은 OAuthService(Orchestrator)로 이동한다.
 * - 동기(RestTemplate) 호출이므로 ThreadPoolBulkhead 대신 SemaphoreBulkhead로 "동시성 제한"만 적용한다.
 */
@Service
public class KakaoOAuthProvider implements OAuthProvider {

	private static final String KAKAO_CIRCUIT_BREAKER = "kakao";
	private static final String KAKAO_BULKHEAD = "kakao";

	private final KakaoOAuthProperties properties;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final Bulkhead kakaoBulkhead;

	public KakaoOAuthProvider(
		KakaoOAuthProperties properties,
		RestTemplateBuilder restTemplateBuilder,
		ObjectMapper objectMapper,
		BulkheadRegistry bulkheadRegistry
	) {
		this.properties = properties;

		Duration connectTimeout = properties.connectTimeout() != null
			? properties.connectTimeout()
			: Duration.ofSeconds(2);
		Duration readTimeout = properties.readTimeout() != null
			? properties.readTimeout()
			: Duration.ofSeconds(3);
		this.restTemplate = restTemplateBuilder.connectTimeout(connectTimeout).readTimeout(readTimeout)
			.build();

		this.objectMapper = objectMapper;
		this.kakaoBulkhead = bulkheadRegistry.bulkhead(KAKAO_BULKHEAD);
	}

	@Override
	public OAuthProviderType type() {
		return OAuthProviderType.KAKAO;
	}

	@Override
	public String buildAuthorizeUrl(String state) {
		validateOauthConfig();

		return UriComponentsBuilder.fromUriString(properties.authorizeUrl())
			.queryParam("response_type", "code")
			.queryParam("client_id", properties.clientId())
			.queryParam("redirect_uri", properties.redirectUri())
			.queryParam("state", state)
			.toUriString();
	}

	@Override
	@CircuitBreaker(name = KAKAO_CIRCUIT_BREAKER, fallbackMethod = "requestTokenFallback")
	public OAuthToken requestToken(String code) {
		validateOauthConfig();

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", properties.clientId());
		body.add("redirect_uri", properties.redirectUri());
		body.add("code", code);
		if (properties.clientSecret() != null && !properties.clientSecret().isBlank()) {
			body.add("client_secret", properties.clientSecret());
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		try {
			ResponseEntity<JsonNode> response = executeWithKakaoBulkhead(() ->
				restTemplate.postForEntity(
					properties.tokenUrl(),
					request,
					JsonNode.class
				)
			);

			JsonNode root = response.getBody();
			String accessToken = (root != null) ? root.path("access_token").asText(null) : null;
			if (accessToken == null || accessToken.isBlank()) {
				throw new ApiException(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
			}
			return new OAuthToken(accessToken);
		} catch (RestClientResponseException ex) {
			throw new ApiException(
				AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
				ex.getResponseBodyAsString()
			);
		}
	}

	@Override
	@CircuitBreaker(name = KAKAO_CIRCUIT_BREAKER, fallbackMethod = "requestUserInfoFallback")
	public OAuthUserInfo requestUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = executeWithKakaoBulkhead(() ->
				restTemplate.exchange(
					properties.userInfoUrl(),
					HttpMethod.GET,
					request,
					String.class
				)
			);

			String responseBody = response.getBody();
			if (responseBody == null || responseBody.isBlank()) {
				throw new ApiException(AuthErrorCode.KAKAO_USERINFO_REQUEST_FAILED);
			}

			JsonNode root = objectMapper.readTree(responseBody);
			Long id = root.path("id").isNumber() ? root.path("id").asLong() : null;
			if (id == null) {
				throw new ApiException(AuthErrorCode.KAKAO_USERINFO_REQUEST_FAILED, "missing_id");
			}
			JsonNode profileNode = root.path("kakao_account").path("profile");
			String nickname = profileNode.path("nickname").asText(null);
			String thumbnailImageUrl = profileNode.path("thumbnail_image_url").asText(null);
			String profileImageUrl = profileNode.path("profile_image_url").asText(null);

			return new OAuthUserInfo(String.valueOf(id), nickname, thumbnailImageUrl, profileImageUrl);
		} catch (RestClientResponseException ex) {
			throw new ApiException(
				AuthErrorCode.KAKAO_USERINFO_REQUEST_FAILED,
				ex.getResponseBodyAsString()
			);
		} catch (IOException ex) {
			throw new ApiException(AuthErrorCode.INTERNAL_SERVER_ERROR, ex.getMessage());
		}
	}

	@Override
	@CircuitBreaker(name = KAKAO_CIRCUIT_BREAKER, fallbackMethod = "unlinkFallback")
	public void unlink(String providerMemberId) {
		if (providerMemberId == null || providerMemberId.isBlank()) {
			throw new ApiException(AuthErrorCode.INVALID_REQUEST, "providerMemberId is required");
		}
		if (properties.adminKey() == null || properties.adminKey().isBlank()) {
			throw new ApiException(AuthErrorCode.INTERNAL_SERVER_ERROR, "Missing Kakao admin key");
		}

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("target_id_type", "user_id");
		body.add("target_id", providerMemberId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", "KakaoAK " + properties.adminKey());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		try {
			ResponseEntity<String> response = executeWithKakaoBulkhead(() ->
				restTemplate.postForEntity(
					properties.unlinkUrl(),
					request,
					String.class
				)
			);
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new ApiException(AuthErrorCode.KAKAO_UNLINK_FAILED);
			}
		} catch (RestClientResponseException ex) {
			throw new ApiException(
				AuthErrorCode.KAKAO_UNLINK_FAILED,
				ex.getResponseBodyAsString()
			);
		}
	}

	@Override
	public String frontendRedirectUrl() {
		return properties.frontendRedirectUrl();
	}

	private void validateOauthConfig() {
		if (properties.clientId() == null || properties.clientId().isBlank()
			|| properties.redirectUri() == null || properties.redirectUri().isBlank()) {
			throw new ApiException(
				AuthErrorCode.INTERNAL_SERVER_ERROR,
				"Missing Kakao OAuth configuration"
			);
		}
	}

	private OAuthToken requestTokenFallback(String code, Throwable throwable) {
		throw translateKakaoException(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED, throwable);
	}

	private OAuthUserInfo requestUserInfoFallback(String accessToken, Throwable throwable) {
		throw translateKakaoException(AuthErrorCode.KAKAO_USERINFO_REQUEST_FAILED, throwable);
	}

	private void unlinkFallback(String providerMemberId, Throwable throwable) {
		throw translateKakaoException(AuthErrorCode.KAKAO_UNLINK_FAILED, throwable);
	}

	private <T> T executeWithKakaoBulkhead(Supplier<T> supplier) {
		// SemaphoreBulkhead: 호출 스레드는 그대로 두고, "동시 호출 수"만 제한한다.
		return kakaoBulkhead.executeSupplier(supplier);
	}

	private ApiException translateKakaoException(AuthErrorCode errorCode, Throwable throwable) {
		if (throwable instanceof CallNotPermittedException) {
			return new ApiException(AuthErrorCode.KAKAO_CIRCUIT_OPEN);
		}
		if (throwable instanceof BulkheadFullException) {
			return new ApiException(AuthErrorCode.KAKAO_BULKHEAD_FULL);
		}
		if (throwable instanceof ApiException apiException) {
			return apiException;
		}
		return new ApiException(errorCode, throwable.getMessage());
	}
}

