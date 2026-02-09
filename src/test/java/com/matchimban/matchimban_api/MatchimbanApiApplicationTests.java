package com.matchimban.matchimban_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 통합 테스트: 스프링 부트 컨텍스트가 정상적으로 뜨는지만 확인한다.
// @SpringBootTest는 애플리케이션 전체 컨텍스트를 올려서 통합 테스트를 수행한다.
@SpringBootTest
class MatchimbanApiApplicationTests {

	@Test
	// @Test는 JUnit5에서 테스트 메서드임을 표시한다.
	// 메서드 본문이 비어 있으면 "컨텍스트 로딩 자체가 실패하지 않는지"만 체크한다.
	void contextLoads() {
	}

}
