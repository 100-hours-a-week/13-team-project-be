package com.matchimban.matchimban_api.auth.kakao.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchimban.matchimban_api.auth.kakao.config.KakaoOAuthProperties;
import com.matchimban.matchimban_api.global.error.ApiException;
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

    // 단위 테스트: 예외 매핑 로직만 집중적으로 검증
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

        return new KakaoAuthServiceImpl(
                props,
                new RestTemplateBuilder(),
                new ObjectMapper(),
                mock(StringRedisTemplate.class)
        );
    }

    @Test
    void 타임아웃_예외는_배드게이트웨이로_매핑된다() {
        KakaoAuthServiceImpl service = buildService();
        ResourceAccessException timeout = new ResourceAccessException("timeout");

        // 내부 예외 매핑 로직을 직접 호출하여 타임아웃 처리 결과 검증
        ApiException api = ReflectionTestUtils.invokeMethod(
                service,
                "translateKakaoException",
                "kakao_token_request_failed",
                timeout
        );

        assertThat(api.getStatus().value()).isEqualTo(502);
    }

    @Test
    void 서킷오픈은_서비스불가로_매핑된다() {
        KakaoAuthServiceImpl service = buildService();
        CircuitBreaker breaker = CircuitBreaker.ofDefaults("kakao");
        CallNotPermittedException open =
                CallNotPermittedException.createCallNotPermittedException(breaker);

        // 서킷이 OPEN일 때는 즉시 서비스 불가로 매핑되는지 확인
        ApiException api = ReflectionTestUtils.invokeMethod(
                service,
                "translateKakaoException",
                "kakao_token_request_failed",
                open
        );

        assertThat(api.getStatus().value()).isEqualTo(503);
        assertThat(api.getMessage()).isEqualTo("kakao_circuit_open");
    }
}
