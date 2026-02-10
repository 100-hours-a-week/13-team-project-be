package com.matchimban.matchimban_api.auth.kakao.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.kakao.config.KakaoOAuthProperties;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoOAuthProviderUnitTest {

	// 단위 테스트: 예외 매핑 로직(translateKakaoException)만 빠르게 검증한다.
	private KakaoOAuthProvider buildProvider() {
		KakaoOAuthProperties props = new KakaoOAuthProperties(
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			Duration.ofSeconds(1),
			Duration.ofSeconds(1)
		);

		return new KakaoOAuthProvider(
			props,
			new RestTemplateBuilder(),
			new ObjectMapper(),
			BulkheadRegistry.ofDefaults()
		);
	}

	@Test
	void timeoutMapsToBadGateway() {
		KakaoOAuthProvider provider = buildProvider();
		ResourceAccessException timeout = new ResourceAccessException("timeout");

		ApiException api = ReflectionTestUtils.invokeMethod(
			provider,
			"translateKakaoException",
			AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
			timeout
		);

		assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
		assertThat(api.getErrorCode().getStatus().value()).isEqualTo(502);
	}

	@Test
	void circuitOpenMapsToServiceUnavailable() {
		KakaoOAuthProvider provider = buildProvider();
		CircuitBreaker breaker = CircuitBreaker.ofDefaults("kakao");
		CallNotPermittedException open =
			CallNotPermittedException.createCallNotPermittedException(breaker);

		ApiException api = ReflectionTestUtils.invokeMethod(
			provider,
			"translateKakaoException",
			AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
			open
		);

		assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_CIRCUIT_OPEN);
		assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
	}

	@Test
	void bulkheadFullMapsToServiceUnavailable() {
		KakaoOAuthProvider provider = buildProvider();
		Bulkhead bulkhead = BulkheadRegistry.ofDefaults().bulkhead("kakao");
		BulkheadFullException full = BulkheadFullException.createBulkheadFullException(bulkhead);

		ApiException api = ReflectionTestUtils.invokeMethod(
			provider,
			"translateKakaoException",
			AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
			full
		);

		assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_BULKHEAD_FULL);
		assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
	}
}

