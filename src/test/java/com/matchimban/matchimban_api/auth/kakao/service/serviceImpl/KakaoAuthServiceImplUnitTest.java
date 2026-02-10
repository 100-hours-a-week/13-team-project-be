package com.matchimban.matchimban_api.auth.kakao.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.kakao.config.KakaoOAuthProperties;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KakaoAuthServiceImplUnitTest {

    // 단위 테스트: 예외 매핑 로직(translateKakaoException)만 집중적으로 검증한다.
    // 단위 테스트는 외부 I/O 없이, 작은 로직 단위를 빠르게 검증하는 목적이다.
    private KakaoAuthServiceImpl buildService() {
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

        // mock(...)은 실제 구현 대신 가짜 객체를 만들어 의존성을 최소화한다.
        return new KakaoAuthServiceImpl(
                props,
                new RestTemplateBuilder(),
                new ObjectMapper(),
                mock(StringRedisTemplate.class),
                BulkheadRegistry.ofDefaults()
        );
    }

    @Test
    // @Test: JUnit5 테스트 메서드 표시
    void timeoutMapsToBadGateway() {
        KakaoAuthServiceImpl service = buildService();
        ResourceAccessException timeout = new ResourceAccessException("timeout");

        // ReflectionTestUtils.invokeMethod는 private 메서드를 테스트용으로 직접 호출한다.
        // AuthErrorCode로 매핑되는지 확인
        ApiException api = ReflectionTestUtils.invokeMethod(
                service,
                "translateKakaoException",
                AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
                timeout
        );

        // assertThat(...).isEqualTo(...)는 AssertJ 문법: 기대값과 실제값 비교
        assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        assertThat(api.getErrorCode().getStatus().value()).isEqualTo(502);
    }

    @Test
    // @Test: JUnit5 테스트 메서드 표시
    void circuitOpenMapsToServiceUnavailable() {
        KakaoAuthServiceImpl service = buildService();
        // CircuitBreaker.ofDefaults(...)는 기본 설정의 서킷브레이커 인스턴스를 만든다.
        CircuitBreaker breaker = CircuitBreaker.ofDefaults("kakao");
        CallNotPermittedException open =
                CallNotPermittedException.createCallNotPermittedException(breaker);

        // 서킷이 OPEN일 때는 즉시 서비스 불가(503)로 매핑되는지 확인
        ApiException api = ReflectionTestUtils.invokeMethod(
                service,
                "translateKakaoException",
                AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
                open
        );

        assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_CIRCUIT_OPEN);
        assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
    }

    @Test
    // @Test: JUnit5 테스트 메서드 표시
    void bulkheadFullMapsToServiceUnavailable() {
        KakaoAuthServiceImpl service = buildService();
        // BulkheadRegistry.ofDefaults()는 기본 벌크헤드 레지스트리를 생성한다.
        Bulkhead bulkhead = BulkheadRegistry.ofDefaults().bulkhead("kakao");
        // BulkheadFullException은 벌크헤드가 꽉 찼을 때 발생하는 예외이다.
        BulkheadFullException full = BulkheadFullException.createBulkheadFullException(bulkhead);

        ApiException api = ReflectionTestUtils.invokeMethod(
                service,
                "translateKakaoException",
                AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
                full
        );

        assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_BULKHEAD_FULL);
        assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
    }
}
