package com.matchimban.matchimban_api.auth.kakao.service.serviceImpl;

import com.matchimban.matchimban_api.MatchimbanApiApplication;
import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoTokenResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;
import com.matchimban.matchimban_api.auth.kakao.service.KakaoAuthService;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 통합 테스트: 스프링 컨텍스트를 실제로 올리고, RestTemplate 호출을 Mock 서버로 대체해 검증한다.
// @SpringBootTest는 애플리케이션 전체 빈을 로딩하는 통합 테스트용 애노테이션이다.
@SpringBootTest(
        classes = MatchimbanApiApplication.class,
        properties = {
                "spring.task.scheduling.enabled=false",
                "kakao.oauth.authorize-url=http://kakao.test/oauth/authorize",
                "kakao.oauth.token-url=http://kakao.test/oauth/token",
                "kakao.oauth.user-info-url=http://kakao.test/v2/user/me",
                "kakao.oauth.unlink-url=http://kakao.test/v1/user/unlink",
                "kakao.oauth.client-id=test-client",
                "kakao.oauth.redirect-uri=http://localhost/callback",
                // 테스트에서 벌크헤드 포화를 쉽게 재현하기 위해 동시성 제한을 작게 설정
                "resilience4j.bulkhead.instances.kakao.max-concurrent-calls=1",
                "resilience4j.bulkhead.instances.kakao.max-wait-duration=0"
        }
)
class KakaoAuthServiceImplIntegrationTest {

    @Autowired
    // @Autowired는 스프링이 관리하는 빈을 주입한다.
    private KakaoAuthService kakaoAuthService;

    @MockitoBean
    // @MockitoBean은 스프링 컨텍스트에 실제 빈 대신 Mockito mock을 넣는다.
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    private MockRestServiceServer server;

    @BeforeEach
    // @BeforeEach는 각 테스트 전에 실행되는 초기화 메서드이다.
    void setUp() throws Exception {
        // 테스트 간 서킷 상태 공유 방지
        circuitBreakerRegistry.circuitBreaker("kakao").reset();

        // AOP 프록시 내부의 실제 객체를 꺼내기 위해 AopTestUtils 사용
        // ReflectionTestUtils.getField로 private 필드(RestTemplate)를 접근
        // MockRestServiceServer로 실제 HTTP 대신 가짜 응답을 구성
        KakaoAuthServiceImpl target = AopTestUtils.getTargetObject(kakaoAuthService);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(target, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    // 단위가 아니라 통합 테스트: 실제 빈 조합 + MockRestServiceServer로 외부 호출을 대체
    void tokenRequestSuccess() {
        // server.expect(...)는 "이 요청이 와야 한다"는 기대치를 설정
        server.expect(requestTo("http://kakao.test/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-1",
                          "token_type": "bearer",
                          "refresh_token": "refresh-1",
                          "expires_in": 7200
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoTokenResponse response = kakaoAuthService.requestToken("code");

        // AssertJ: 실제 응답 값이 기대값과 동일한지 확인
        assertThat(response.accessToken()).isEqualTo("access-1");
        // server.verify()로 기대한 요청이 실제로 발생했는지 검증
        server.verify();
    }

    @Test
    // 단위가 아니라 통합 테스트: 외부 응답을 Mock으로 주고 파싱 로직 검증
    void userInfoRequestSuccess() {
        server.expect(requestTo("http://kakao.test/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": 123,
                          "kakao_account": {
                            "profile": {
                              "nickname": "MockUser",
                              "thumbnail_image_url": "https://example.com/thumb.png",
                              "profile_image_url": "https://example.com/profile.png"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoUserInfo userInfo = kakaoAuthService.requestUserInfo("access");

        assertThat(userInfo.id()).isEqualTo(123L);
        assertThat(userInfo.nickname()).isEqualTo("MockUser");
        server.verify();
    }

    @Test
    // 실패 응답(500)이 들어오면 ApiException(502)로 매핑되는지 확인
    void tokenRequestFailureBadGateway() {
        server.expect(requestTo("http://kakao.test/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // assertThatThrownBy는 "예외가 발생해야 한다"는 검증 문법
        // .satisfies(...)로 예외 내부 값까지 상세 검증 가능
        assertThatThrownBy(() -> kakaoAuthService.requestToken("code"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
                    assertThat(api.getErrorCode().getStatus().value()).isEqualTo(502);
                });

        server.verify();
    }

    @Test
    // 벌크헤드가 포화되면 실제 요청이 거절되는지 확인하는 통합 테스트
    void bulkheadFullRejectsRequest() throws Exception {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("kakao");
        // CountDownLatch는 멀티스레드 동기화 도구
        // started: 작업 시작 신호, release: 작업 종료 신호
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // 첫 번째 호출로 벌크헤드 permit을 점유시키고, 일부러 오래 점유시킴
        CompletableFuture<Void> blocker = CompletableFuture.runAsync(() ->
                bulkhead.executeRunnable(() -> {
                    started.countDown();
                    try {
                        release.await(3, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                })
        );

        // 첫 번째 작업이 실제로 시작될 때까지 대기
        assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

        // 두 번째 호출은 벌크헤드가 꽉 찬 상태라 503으로 거절되어야 함
        assertThatThrownBy(() -> kakaoAuthService.requestToken("code"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_BULKHEAD_FULL);
                    assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
                });

        // 막아둔 첫 번째 작업을 종료시켜 자원 정리
        release.countDown();
        blocker.get(1, TimeUnit.SECONDS);
    }
}
