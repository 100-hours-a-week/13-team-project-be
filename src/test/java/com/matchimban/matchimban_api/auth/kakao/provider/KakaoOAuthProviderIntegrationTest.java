package com.matchimban.matchimban_api.auth.kakao.provider;

import com.matchimban.matchimban_api.MatchimbanApiApplication;
import com.matchimban.matchimban_api.auth.error.AuthErrorCode;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthProviderType;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthToken;
import com.matchimban.matchimban_api.auth.oauth.model.OAuthUserInfo;
import com.matchimban.matchimban_api.auth.oauth.provider.OAuthProvider;
import com.matchimban.matchimban_api.auth.oauth.provider.OAuthProviderRegistry;
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
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
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
		"kakao.oauth.redirect-uri=http://localhost/callback",
		"resilience4j.bulkhead.instances.kakao.max-concurrent-calls=1",
		"resilience4j.bulkhead.instances.kakao.max-wait-duration=0"
	}
)
class KakaoOAuthProviderIntegrationTest {

	@Autowired
	private OAuthProviderRegistry providerRegistry;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private BulkheadRegistry bulkheadRegistry;

	private OAuthProvider kakaoProvider;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		circuitBreakerRegistry.circuitBreaker("kakao").reset();

		kakaoProvider = providerRegistry.get(OAuthProviderType.KAKAO);
		KakaoOAuthProvider target = AopTestUtils.getTargetObject(kakaoProvider);
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(target, "restTemplate");
		server = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Test
	void tokenRequestSuccess() {
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

		OAuthToken response = kakaoProvider.requestToken("code");

		assertThat(response.accessToken()).isEqualTo("access-1");
		server.verify();
	}

	@Test
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

		OAuthUserInfo userInfo = kakaoProvider.requestUserInfo("access");

		assertThat(userInfo.providerMemberId()).isEqualTo("123");
		assertThat(userInfo.nickname()).isEqualTo("MockUser");
		server.verify();
	}

	@Test
	void tokenRequestFailureBadGateway() {
		server.expect(requestTo("http://kakao.test/oauth/token"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());

		assertThatThrownBy(() -> kakaoProvider.requestToken("code"))
			.isInstanceOf(ApiException.class)
			.satisfies(ex -> {
				ApiException api = (ApiException) ex;
				assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
				assertThat(api.getErrorCode().getStatus().value()).isEqualTo(502);
			});

		server.verify();
	}

	@Test
	void bulkheadFullRejectsRequest() throws Exception {
		Bulkhead bulkhead = bulkheadRegistry.bulkhead("kakao");
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

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

		assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

		assertThatThrownBy(() -> kakaoProvider.requestToken("code"))
			.isInstanceOf(ApiException.class)
			.satisfies(ex -> {
				ApiException api = (ApiException) ex;
				assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_BULKHEAD_FULL);
				assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
			});

		release.countDown();
		blocker.get(1, TimeUnit.SECONDS);
	}
}

