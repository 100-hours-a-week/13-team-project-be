package com.matchimban.matchimban_api.auth.kakao.service.serviceImpl;

import com.matchimban.matchimban_api.MatchimbanApiApplication;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoTokenResponse;
import com.matchimban.matchimban_api.auth.kakao.dto.KakaoUserInfo;
import com.matchimban.matchimban_api.auth.kakao.service.KakaoAuthService;
import com.matchimban.matchimban_api.global.error.ApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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

@SpringBootTest(
        classes = MatchimbanApiApplication.class,
        properties = {
                "spring.task.scheduling.enabled=false",
                "kakao.oauth.authorize-url=http://kakao.test/oauth/authorize",
                "kakao.oauth.token-url=http://kakao.test/oauth/token",
                "kakao.oauth.user-info-url=http://kakao.test/v2/user/me",
                "kakao.oauth.unlink-url=http://kakao.test/v1/user/unlink",
                "kakao.oauth.client-id=test-client",
                "kakao.oauth.redirect-uri=http://localhost/callback"
        }
)
class KakaoAuthServiceImplIntegrationTest {

    @Autowired
    private KakaoAuthService kakaoAuthService;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 간 서킷 상태 공유 방지
        circuitBreakerRegistry.circuitBreaker("kakao").reset();

        // 프록시 대상 객체에서 RestTemplate을 꺼내 MockRestServiceServer에 연결
        KakaoAuthServiceImpl target = AopTestUtils.getTargetObject(kakaoAuthService);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(target, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void 토큰요청_성공() {
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

        assertThat(response.accessToken()).isEqualTo("access-1");
        server.verify();
    }

    @Test
    void 유저정보요청_성공() {
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
    void 토큰요청_실패시_배드게이트웨이_예외() {
        server.expect(requestTo("http://kakao.test/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // 카카오 오류 응답은 ApiException(BAD_GATEWAY)로 매핑되어야 함
        assertThatThrownBy(() -> kakaoAuthService.requestToken("code"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(502);
                });

        server.verify();
    }
}
