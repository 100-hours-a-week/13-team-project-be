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

// "통합 테스트(Integration Test)"는 스프링 컨텍스트를 실제로 띄우고,
// 여러 Bean이 조합된 상태에서 동작을 검증한다.
@SpringBootTest(
	classes = MatchimbanApiApplication.class,
	properties = {
		// 스케줄러/배치가 있으면 테스트를 방해할 수 있어 비활성화한다.
		"spring.task.scheduling.enabled=false",
		// 실제 카카오가 아니라, 테스트용 가짜 URL로 외부 호출을 보낸다.
		// (MockRestServiceServer가 이 URL로 나가는 RestTemplate 호출을 가로챈다.)
		"kakao.oauth.authorize-url=http://kakao.test/oauth/authorize",
		"kakao.oauth.token-url=http://kakao.test/oauth/token",
		"kakao.oauth.user-info-url=http://kakao.test/v2/user/me",
		"kakao.oauth.unlink-url=http://kakao.test/v1/user/unlink",
		"kakao.oauth.client-id=test-client",
		"kakao.oauth.redirect-uri=http://localhost/callback",
		// 벌크헤드 포화 상황을 테스트에서 쉽게 만들기 위해 동시성 제한을 1로 낮춘다.
		"resilience4j.bulkhead.instances.kakao.max-concurrent-calls=1",
		// 대기 없이 즉시 실패하도록 설정해서 "두 번째 호출이 바로 거절되는지"를 검증한다.
		"resilience4j.bulkhead.instances.kakao.max-wait-duration=0"
	}
)
class KakaoOAuthProviderIntegrationTest {

	@Autowired
	// @Autowired: 스프링 컨텍스트에 등록된 Bean을 주입받는다.
	private OAuthProviderRegistry providerRegistry;

	@MockitoBean
	// @MockitoBean: 실제 Bean 대신 Mockito mock을 스프링 컨텍스트에 주입한다.
	// (테스트가 실제 Redis에 연결되지 않도록 안전장치로 넣어둔다.)
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	// 서킷브레이커는 테스트 간 상태가 공유될 수 있어 reset 용도로 주입받는다.
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private BulkheadRegistry bulkheadRegistry;

	private OAuthProvider kakaoProvider;
	private MockRestServiceServer server;

	@BeforeEach
	// @BeforeEach: 각 테스트 메서드 실행 전에 매번 호출된다.
	void setUp() {
		// 테스트 간 서킷 상태(OPEN/HALF_OPEN)가 섞이지 않도록 초기화한다.
		circuitBreakerRegistry.circuitBreaker("kakao").reset();

		// Registry에서 "KAKAO" Provider(Strategy)를 꺼낸다.
		kakaoProvider = providerRegistry.get(OAuthProviderType.KAKAO);

		// Resilience4j @CircuitBreaker가 붙어 있으면 Provider 빈이 AOP 프록시가 될 수 있다.
		// AopTestUtils.getTargetObject(...)로 프록시 내부의 실제 객체(KakaoOAuthProvider)를 꺼낸다.
		KakaoOAuthProvider target = AopTestUtils.getTargetObject(kakaoProvider);

		// ReflectionTestUtils.getField(...)는 private 필드에 리플렉션으로 접근한다.
		// Provider 내부 RestTemplate을 꺼내서 MockRestServiceServer를 붙이는 목적이다.
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(target, "restTemplate");

		// MockRestServiceServer는 RestTemplate의 HTTP 호출을 "진짜 네트워크" 대신 "가짜 응답"으로 대체한다.
		server = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Test
	void tokenRequestSuccess() {
		// given: token 엔드포인트로 POST 요청이 오면, 아래 JSON을 200 OK로 응답하도록 세팅한다.
		// server.expect(...)는 "이 요청이 반드시 발생해야 한다"는 기대치를 등록하는 API다.
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

		// when: 실제 Provider 메서드를 호출한다. (내부적으로 RestTemplate이 호출되지만, Mock 서버가 가로챈다.)
		OAuthToken response = kakaoProvider.requestToken("code");

		// then: 응답 파싱 결과가 기대값과 같은지 검증한다.
		// assertThat(...)는 AssertJ 검증 문법이다.
		assertThat(response.accessToken()).isEqualTo("access-1");
		// server.verify()는 "기대했던 요청이 실제로 발생했는지"를 최종 확인한다.
		server.verify();
	}

	@Test
	void userInfoRequestSuccess() {
		// given: userinfo 엔드포인트로 GET 요청이 오면, 아래 JSON을 200 OK로 응답하도록 세팅한다.
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

		// when: access token을 넣어 userinfo 요청을 수행한다.
		OAuthUserInfo userInfo = kakaoProvider.requestUserInfo("access");

		// then: Provider가 JSON을 공통 모델(OAuthUserInfo)로 변환한 값이 맞는지 확인한다.
		assertThat(userInfo.providerMemberId()).isEqualTo("123");
		assertThat(userInfo.nickname()).isEqualTo("MockUser");
		server.verify();
	}

	@Test
	void tokenRequestFailureBadGateway() {
		// given: token 호출이 500 에러를 반환하도록 구성한다.
		server.expect(requestTo("http://kakao.test/oauth/token"))
			.andExpect(method(HttpMethod.POST))
			.andRespond(withServerError());

		// when/then: 예외가 발생해야 한다는 것을 검증한다.
		// assertThatThrownBy(...)는 "이 람다 실행 시 예외가 던져져야 한다"는 AssertJ 문법이다.
		assertThatThrownBy(() -> kakaoProvider.requestToken("code"))
			.isInstanceOf(ApiException.class)
			// satisfies(...)는 예외 내부 필드까지 더 자세히 검사하고 싶을 때 사용한다.
			.satisfies(ex -> {
				// ex는 Object 타입으로 들어오므로 ApiException으로 캐스팅해서 상세 값을 확인한다.
				ApiException api = (ApiException) ex;
				assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
				assertThat(api.getErrorCode().getStatus().value()).isEqualTo(502);
			});

		server.verify();
	}

	@Test
	void bulkheadFullRejectsRequest() throws Exception {
		// given: bulkhead 설정이 max-concurrent-calls=1 이므로,
		// 한 번 permit을 잡고 있으면 두 번째 호출은 즉시 거절되어야 한다.
		Bulkhead bulkhead = bulkheadRegistry.bulkhead("kakao");

		// CountDownLatch는 멀티스레드 동기화 도구다.
		// started: "첫 번째 작업이 시작되었다" 신호, release: "첫 번째 작업을 끝내라" 신호
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		// CompletableFuture.runAsync(...)는 별도 스레드에서 작업을 실행한다.
		// 여기서는 첫 번째 작업이 bulkhead permit을 잡은 채로 오래 대기하도록 만든다.
		CompletableFuture<Void> blocker = CompletableFuture.runAsync(() ->
			bulkhead.executeRunnable(() -> {
				// started.countDown(): 메인 스레드에게 "permit 점유 완료"를 알려준다.
				started.countDown();
				try {
					// release가 열릴 때까지 대기해서 permit을 계속 점유한다.
					release.await(3, TimeUnit.SECONDS);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			})
		);

		// 첫 번째 작업이 실제로 시작(permit 점유)될 때까지 최대 1초 기다린다.
		assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

		// when/then: 이제 두 번째 호출은 BulkheadFull로 거절되어 503으로 매핑되어야 한다.
		assertThatThrownBy(() -> kakaoProvider.requestToken("code"))
			.isInstanceOf(ApiException.class)
			.satisfies(ex -> {
				ApiException api = (ApiException) ex;
				assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_BULKHEAD_FULL);
				assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
			});

		// 정리: permit을 점유 중이던 첫 번째 작업을 풀어주고, future가 정상 종료되었는지 확인한다.
		release.countDown();
		blocker.get(1, TimeUnit.SECONDS);
	}
}
