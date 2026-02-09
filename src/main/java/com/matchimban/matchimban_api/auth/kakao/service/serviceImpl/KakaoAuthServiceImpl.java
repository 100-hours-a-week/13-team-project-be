package com.matchimban.matchimban_api.auth.kakao.service.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.auth.kakao.config.KakaoOAuthProperties;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoTokenResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;
import com.matchimban.matchimban_api.auth.kakao.service.KakaoAuthService;
import com.matchimban.matchimban_api.global.error.ApiException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KakaoAuthServiceImpl implements KakaoAuthService {
	private static final Duration STATE_TTL = Duration.ofMinutes(5);
	private static final String OAUTH_STATE_KEY_PREFIX = "oauth_state:";
	private static final String KAKAO_CIRCUIT_BREAKER = "kakao";
	private static final String KAKAO_THREADPOOL_BULKHEAD = "kakao";

	private final KakaoOAuthProperties properties;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate stringRedisTemplate;
	// 카카오 호출 전용 스레드풀 벌크헤드(동기 RestTemplate 호출을 별도 풀로 격리)
	private final ThreadPoolBulkhead kakaoThreadPoolBulkhead;

	public KakaoAuthServiceImpl(
		KakaoOAuthProperties properties,
		RestTemplateBuilder restTemplateBuilder,
		ObjectMapper objectMapper,
		StringRedisTemplate stringRedisTemplate,
		ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry
	) {
		this.properties = properties;
		// 외부 카카오 장애 시 서블릿 스레드가 오래 묶이지 않도록 타임아웃 설정.
		Duration connectTimeout = properties.connectTimeout() != null
			? properties.connectTimeout()
			: Duration.ofSeconds(2);
		Duration readTimeout = properties.readTimeout() != null
			? properties.readTimeout()
			: Duration.ofSeconds(3);
		this.restTemplate = restTemplateBuilder.connectTimeout(connectTimeout).readTimeout(readTimeout)
			.build();
		this.objectMapper = objectMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		// 카카오 호출 전용 스레드풀 벌크헤드 사용 (전용 풀로 격리)
		// - 서블릿 스레드가 외부 호출로 잠기는 것을 방지
		// - 점심 피크 등 트래픽 몰림에서 전체 API가 멈추는 상황을 완화
		this.kakaoThreadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(KAKAO_THREADPOOL_BULKHEAD);
	}

	@Override
	public String issueState() {
		String state = UUID.randomUUID().toString();
		stringRedisTemplate.opsForValue().set(buildOauthStateKey(state), "1", STATE_TTL);
		return state;
	}

	@Override
	public boolean consumeState(String state) {
		if (state == null || state.isBlank()) {
			return false;
		}
		return Boolean.TRUE.equals(stringRedisTemplate.delete(buildOauthStateKey(state)));
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
	// 카카오 장애 시 빠르게 실패하여 연쇄 장애를 방지.
	@CircuitBreaker(name = KAKAO_CIRCUIT_BREAKER, fallbackMethod = "requestTokenFallback")
	public KakaoTokenResponse requestToken(String code) {
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
			// 카카오 API 호출은 전용 스레드풀에서 실행하여 서블릿 스레드 고갈을 방지
			ResponseEntity<KakaoTokenResponse> response = executeWithKakaoBulkhead(() ->
				restTemplate.postForEntity(
					properties.tokenUrl(),
					request,
					KakaoTokenResponse.class
				)
			);
			KakaoTokenResponse tokenResponse = response.getBody();
			if (tokenResponse == null || tokenResponse.accessToken() == null) {
				throw new ApiException(HttpStatus.BAD_GATEWAY, "kakao_token_request_failed");
			}
			return tokenResponse;
		} catch (RestClientResponseException ex) {
			throw new ApiException(
				HttpStatus.BAD_GATEWAY,
				"kakao_token_request_failed",
				ex.getResponseBodyAsString()
			);
		}
	}

	@Override
	// 카카오 장애 시 빠르게 실패하여 연쇄 장애를 방지.
	@CircuitBreaker(name = KAKAO_CIRCUIT_BREAKER, fallbackMethod = "requestUserInfoFallback")
	public KakaoUserInfo requestUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			// 카카오 API 호출은 전용 스레드풀에서 실행하여 서블릿 스레드 고갈을 방지
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
				throw new ApiException(HttpStatus.BAD_GATEWAY, "kakao_userinfo_request_failed");
			}

			JsonNode root = objectMapper.readTree(responseBody);
			Long id = root.path("id").isNumber() ? root.path("id").asLong() : null;
			JsonNode profileNode = root.path("kakao_account").path("profile");
			String nickname = profileNode.path("nickname").asText(null);
			String thumbnailImageUrl = profileNode.path("thumbnail_image_url").asText(null);
			String profileImageUrl = profileNode.path("profile_image_url").asText(null);

			return new KakaoUserInfo(root, id, nickname, thumbnailImageUrl, profileImageUrl);
		} catch (RestClientResponseException ex) {
			throw new ApiException(
				HttpStatus.BAD_GATEWAY,
				"kakao_userinfo_request_failed",
				ex.getResponseBodyAsString()
			);
		} catch (IOException ex) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error", ex.getMessage());
		}
	}

	@Override
	// 카카오 장애 시 빠르게 실패하여 연쇄 장애를 방지.
	@CircuitBreaker(name = KAKAO_CIRCUIT_BREAKER, fallbackMethod = "unlinkByAdminKeyFallback")
	public void unlinkByAdminKey(String providerMemberId) {
		// 관리자 키 방식으로 카카오 연결 해제 (토큰 폐기 + 동의 철회)
		if (providerMemberId == null || providerMemberId.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", "providerMemberId is required");
		}
		if (properties.adminKey() == null || properties.adminKey().isBlank()) {
			throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error", "Missing Kakao admin key");
		}

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("target_id_type", "user_id");
		body.add("target_id", providerMemberId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.set("Authorization", "KakaoAK " + properties.adminKey());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		try {
			// 카카오 API 호출은 전용 스레드풀에서 실행하여 서블릿 스레드 고갈을 방지
			ResponseEntity<String> response = executeWithKakaoBulkhead(() ->
				restTemplate.postForEntity(
					properties.unlinkUrl(),
					request,
					String.class
				)
			);
			// 카카오 응답이 2xx가 아니면 실패로 처리
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new ApiException(HttpStatus.BAD_GATEWAY, "kakao_unlink_failed");
			}
		} catch (RestClientResponseException ex) {
			// 카카오 응답 바디를 포함해 에러로 전파
			throw new ApiException(
				HttpStatus.BAD_GATEWAY,
				"kakao_unlink_failed",
				ex.getResponseBodyAsString()
			);
		}
	}

	private void validateOauthConfig() {
		if (properties.clientId() == null || properties.clientId().isBlank()
			|| properties.redirectUri() == null || properties.redirectUri().isBlank()) {
			throw new ApiException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"internal_server_error",
				"Missing Kakao OAuth configuration"
			);
		}
	}

	private String buildOauthStateKey(String state) {
		return OAUTH_STATE_KEY_PREFIX + state;
	}

	private KakaoTokenResponse requestTokenFallback(String code, Throwable throwable) {
		throw translateKakaoException("kakao_token_request_failed", throwable);
	}

	private KakaoUserInfo requestUserInfoFallback(String accessToken, Throwable throwable) {
		throw translateKakaoException("kakao_userinfo_request_failed", throwable);
	}

	private void unlinkByAdminKeyFallback(String providerMemberId, Throwable throwable) {
		throw translateKakaoException("kakao_unlink_failed", throwable);
	}

	private <T> T executeWithKakaoBulkhead(Supplier<T> supplier) {
		try {
			// 전용 스레드풀에서 실행하고 결과를 동기적으로 기다림
			// - 호출 자체는 별도 풀에서 실행
			// - 현재 스레드는 결과만 대기 (서블릿 풀 고갈을 전용 풀로 격리)
			return kakaoThreadPoolBulkhead.executeSupplier(supplier)
				.toCompletableFuture()
				.get();
		} catch (ExecutionException ex) {
			// 실제 예외 원인으로 풀어서 재던짐 (RestTemplate 예외 등)
			Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			// 체크 예외는 공통 에러로 변환
			throw new ApiException(HttpStatus.BAD_GATEWAY, "kakao_request_failed", cause.getMessage());
		} catch (InterruptedException ex) {
			// 인터럽트 플래그 복구 후 서비스 불가 처리
			Thread.currentThread().interrupt();
			throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "kakao_request_interrupted", ex.getMessage());
		}
	}

	private ApiException translateKakaoException(String message, Throwable throwable) {
		if (throwable instanceof CallNotPermittedException) {
			return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "kakao_circuit_open");
		}
		// 벌크헤드 큐/스레드가 꽉 찬 경우
		if (throwable instanceof BulkheadFullException) {
			return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "kakao_bulkhead_full");
		}
		if (throwable instanceof ApiException apiException) {
			return apiException;
		}
		return new ApiException(HttpStatus.BAD_GATEWAY, message, throwable.getMessage());
	}
}
