package com.matchimban.matchimban_api.auth.kakao.service.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.auth.kakao.config.KakaoOAuthProperties;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoTokenResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;
import com.matchimban.matchimban_api.auth.kakao.service.KakaoAuthService;
import com.matchimban.matchimban_api.global.error.ApiException;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
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

	private final KakaoOAuthProperties properties;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final StringRedisTemplate stringRedisTemplate;

	public KakaoAuthServiceImpl(
		KakaoOAuthProperties properties,
		RestTemplateBuilder restTemplateBuilder,
		ObjectMapper objectMapper,
		StringRedisTemplate stringRedisTemplate
	) {
		this.properties = properties;
		this.restTemplate = restTemplateBuilder.build();
		this.objectMapper = objectMapper;
		this.stringRedisTemplate = stringRedisTemplate;
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
			ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
				properties.tokenUrl(),
				request,
				KakaoTokenResponse.class
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
	public KakaoUserInfo requestUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<Void> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<String> response = restTemplate.exchange(
				properties.userInfoUrl(),
				HttpMethod.GET,
				request,
				String.class
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
			ResponseEntity<String> response = restTemplate.postForEntity(
				properties.unlinkUrl(),
				request,
				String.class
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
}
