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

// "단위 테스트(Unit Test)"는 스프링 컨텍스트를 띄우지 않고,
// 작은 로직(메서드/분기)을 빠르게 검증하는 테스트다.
class KakaoOAuthProviderUnitTest {

	// KakaoOAuthProvider는 생성자 주입을 쓰므로, 테스트에서도 직접 new 해서 만든다.
	// (통합 테스트가 아니므로 @SpringBootTest/@Autowired를 사용하지 않는다.)
	private KakaoOAuthProvider buildProvider() {
		// KakaoOAuthProperties는 @ConfigurationProperties로 바인딩되는 설정 객체다.
		// 여기서는 translateKakaoException만 테스트하므로 대부분 null이어도 된다.
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

		// RestTemplateBuilder/ObjectMapper/BulkheadRegistry는 Provider 생성에 필요한 의존성이다.
		// 실제 HTTP 호출은 하지 않기 때문에 "실제 구현"을 써도 테스트가 외부로 나가지 않는다.
		return new KakaoOAuthProvider(
			props,
			new RestTemplateBuilder(),
			new ObjectMapper(),
			BulkheadRegistry.ofDefaults()
		);
	}

	@Test
	void timeoutMapsToBadGateway() {
		// given: Provider 인스턴스 + "네트워크 타임아웃" 상황을 가정한 예외
		KakaoOAuthProvider provider = buildProvider();
		ResourceAccessException timeout = new ResourceAccessException("timeout");

		// when: private 메서드(translateKakaoException)를 리플렉션으로 직접 호출한다.
		// - 첫 번째 인자: 대상 객체
		// - 두 번째 인자: 호출할 메서드 이름(String)
		// - 그 뒤 인자들: 메서드 파라미터들
		ApiException api = ReflectionTestUtils.invokeMethod(
			provider,
			"translateKakaoException",
			AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
			timeout
		);

		// then: ApiException에 담긴 에러코드/HTTP 상태가 기대대로 매핑되는지 확인한다.
		// assertThat(...)는 AssertJ 문법으로, 가독성 좋은 검증을 제공한다.
		assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
		assertThat(api.getErrorCode().getStatus().value()).isEqualTo(502);
	}

	@Test
	void circuitOpenMapsToServiceUnavailable() {
		// given: 서킷브레이커가 OPEN 상태일 때 던져지는 CallNotPermittedException을 생성한다.
		KakaoOAuthProvider provider = buildProvider();
		CircuitBreaker breaker = CircuitBreaker.ofDefaults("kakao");
		CallNotPermittedException open =
			CallNotPermittedException.createCallNotPermittedException(breaker);

		// when: OPEN 예외를 translateKakaoException에 넘겨 매핑 결과를 확인한다.
		ApiException api = ReflectionTestUtils.invokeMethod(
			provider,
			"translateKakaoException",
			AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
			open
		);

		// then: 서킷 OPEN은 외부 인증 제공자 장애로 보고 503(Service Unavailable)로 매핑한다.
		assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_CIRCUIT_OPEN);
		assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
	}

	@Test
	void bulkheadFullMapsToServiceUnavailable() {
		// given: 세마포어 벌크헤드가 꽉 차면 BulkheadFullException이 발생한다.
		KakaoOAuthProvider provider = buildProvider();
		Bulkhead bulkhead = BulkheadRegistry.ofDefaults().bulkhead("kakao");
		BulkheadFullException full = BulkheadFullException.createBulkheadFullException(bulkhead);

		// when: 벌크헤드 포화 예외를 translateKakaoException에 넘겨 매핑 결과를 확인한다.
		ApiException api = ReflectionTestUtils.invokeMethod(
			provider,
			"translateKakaoException",
			AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED,
			full
		);

		// then: 과부하(동시성 제한) 상황이므로 503(Service Unavailable)로 빠르게 실패한다.
		assertThat(api.getErrorCode()).isEqualTo(AuthErrorCode.KAKAO_BULKHEAD_FULL);
		assertThat(api.getErrorCode().getStatus().value()).isEqualTo(503);
	}
}
