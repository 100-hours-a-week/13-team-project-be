# RAG 챗봇 API(Spring) 설계서

- 작성일: 2026-03-14
- 대상 프로젝트: `be` (`com.matchimban.matchimban_api`)
- 목적: 요청하신 RAG 챗봇 API(`POST /RAGchat`, `DELETE /RAGchat/{user_id}`, `GET /health`, `GET /history/{user_id}`)를 현재 Spring 백엔드 구조에 맞춰 안전하게 서빙

## 1. 설계 원칙 (현재 코드 구조 반영)

1. 레이어 구조 유지
- `controller -> service -> (client/repository) -> dto/error`
- 기존 패턴(`chat`, `vote.ai`)과 동일하게 구성

2. 응답/에러 규약 유지
- 성공 응답은 `ApiResult<T>` 사용
- 실패 응답은 `ApiException + ErrorCode + GlobalExceptionHandler` 사용

3. 인증/식별자 규칙
- 실제 인증 주체는 `@AuthenticationPrincipal MemberPrincipal`
- `user_id`는 외부 계약용 식별자이며, 서버 내부는 `memberId` 기준으로 처리

4. 외부 RAG 연동 방식
- 현재 `vote.ai.RecommendationClient` 패턴처럼 `WebClient` 기반 RAG 전용 클라이언트 구성
- 타임아웃/상태코드 매핑은 서비스 계층에서 정책적으로 처리

## 2. 권장 아키텍처

### 2.1 1차 도입안 (권장): Spring BFF + RAG Engine

- Spring은 API 게이트웨이/정책 계층
- 파이프라인 선택(multistep/multiturn/hyde), SQLite 대화 이력은 RAG 서버가 소유
- Spring은 인증/권한/입력검증/에러표준화/관측성 담당

### 2.2 구성도

```text
[FE]
  -> [RagChatController]
      -> [RagChatService]
          -> [RagEngineClient(WebClient)]
              -> [RAG Server(FastAPI 등)]
                  -> [SQLite]
```

## 3. 패키지/클래스 설계

```text
com.matchimban.matchimban_api.ragchat
├── controller
│   └── RagChatController
├── service
│   ├── RagChatService
│   └── serviceImpl
│       └── RagChatServiceImpl
├── client
│   ├── RagEngineClient
│   └── RagEngineClientConfig
├── dto
│   ├── request
│   │   └── RagChatAskRequest
│   ├── response
│   │   ├── RagChatAskData
│   │   ├── RagHistoryData
│   │   └── RagHistoryMessageItem
│   └── client
│       ├── RagEngineAskRequest
│       ├── RagEngineAskResponse
│       ├── RagEngineHistoryResponse
│       └── RagEngineHealthResponse
└── error
    └── RagChatErrorCode
```

## 4. API 설계 (Spring 노출)

## 4.1 엔드포인트

- `POST /api/v1/rag-chat` (권장 표준 경로)
- `DELETE /api/v1/rag-chat/{user_id}`
- `GET /api/v1/rag-chat/history/{user_id}`
- `GET /api/v1/rag-chat/health`

호환이 필요하면 아래 alias를 추가 매핑
- `POST /RAGchat`
- `DELETE /RAGchat/{user_id}`
- `GET /history/{user_id}`
- `GET /health`

## 4.2 user_id 처리 규칙 (중요)

- 클라이언트가 `user_id`를 보내더라도 서버는 `principal.memberId`로 검증
- 권장 canonical 포맷: `member:{memberId}`
- Path/body `user_id`가 canonical 값과 다르면 `403 FORBIDDEN`

## 4.3 Request/Response 스키마 (서버 외부 계약)

요청하신 원본 계약은 유지 가능하지만, 이 프로젝트 컨벤션상 실제 응답은 아래처럼 `ApiResult` 래핑 권장

예) `POST /api/v1/rag-chat`

```json
{
  "message": "rag_chat_answered",
  "data": {
    "answer": "판교역 근처 주차 가능한 고기집으로 ...",
    "user_id": "member:123"
  }
}
```

예) `GET /api/v1/rag-chat/history/member:123?limit=20&before_id=42`

```json
{
  "message": "rag_history_loaded",
  "data": {
    "messages": [
      {
        "id": 41,
        "role": "user",
        "content": "판교 룸 있는 고기집 추천해줘",
        "created_at": "2026-03-11 10:00:00"
      }
    ],
    "next_cursor": 41
  }
}
```

## 5. 상태코드/에러 정책

요청 스펙에 `408`과 `200(타임아웃 안내)`가 혼재되어 있으므로 아래처럼 고정 권장

1. 검증 실패
- `400` (`user_id`/`message` 누락, 형식 오류)

2. RAG 처리 타임아웃(10초)
- 사용자 경험 우선 정책: `200` + fallback answer
- 예: `"응답 시간이 초과됐어요. 다시 시도해주세요."`

3. 검색 결과 없음
- `200` + fallback answer

4. 내부 예외
- RAG 서버 5xx/네트워크 장애/직렬화 실패: `500`
- `ErrorResponse`로 표준화

5. 이력 파라미터 검증 실패
- `limit`(1~100), `before_id >= 1` 강제
- 프로젝트 표준상 `400 VALIDATION_FAILED` 응답 사용

## 6. 핵심 시퀀스

## 6.1 질문 처리

```text
Controller
  -> Service (memberId -> canonical user_id 변환)
    -> RagEngineClient POST /RAGchat
      -> (정상) answer 반환
      -> (timeout) fallback answer 생성 후 200
      -> (에러) ApiException(RagChatErrorCode.*)
```

## 6.2 히스토리 조회(무한스크롤)

```text
Controller
  -> Service (user_id 검증 + limit/before_id 검증)
    -> RagEngineClient GET /history/{user_id}
      -> messages ASC, next_cursor 반환
```

## 7. 설정값 설계

`application.yml`

```yaml
rag-chat:
  base-url: ${RAG_CHAT_BASE_URL:http://localhost:8001}
  timeout-ms: ${RAG_CHAT_TIMEOUT_MS:10000}
  connect-timeout-ms: ${RAG_CHAT_CONNECT_TIMEOUT_MS:2000}
  read-timeout-ms: ${RAG_CHAT_READ_TIMEOUT_MS:10000}
  paths:
    ask: /RAGchat
    reset: /RAGchat/{user_id}
    history: /history/{user_id}
    health: /health
```

## 8. 보안/스프링 설정 변경점

1. `SecurityConfig`
- `GET /health`를 public으로 열지, 인증필수로 둘지 정책 결정
- public이면 permitAll matcher 추가

2. CSRF
- 상태 변경 API(`POST`, `DELETE`)는 기존 규칙대로 CSRF 헤더 필요
- Swagger 문서에는 `@CsrfRequired` 부착

3. Observability
- 기존 `ApiPerfLoggingFilter` 대상에 자연 포함
- 필요 시 `rag_chat.*` 메트릭 추가

## 9. 구현 순서

1. `ragchat` 패키지/DTO/에러코드 뼈대 생성
2. `RagEngineClient` + 설정 프로퍼티 생성
3. `RagChatServiceImpl`에 타임아웃/에러 매핑 정책 구현
4. `RagChatController` 엔드포인트 + Swagger + `ApiResult` 적용
5. `SecurityConfig` matcher 보완(`GET /health` 정책 반영)
6. 통합테스트: 정상/타임아웃/빈이력/유효성 실패 케이스

## 10. 결정 필요 항목 (착수 전 확정)

1. 외부 공개 경로를 원본 그대로(`/RAGchat`) 쓸지, 표준 경로(`/api/v1/rag-chat`)로 통일할지
2. `user_id`를 클라이언트에서 직접 받을지, `memberId`에서 서버가 강제 생성할지
3. 타임아웃 시 HTTP `200` 유지 여부(요구사항 우선) vs `408` 정합성 우선
4. `GET /health` 공개 여부

---

이 설계는 현재 코드베이스의 규칙(`ApiResult`, `ApiException`, `MemberPrincipal`, `WebClient 외부연동`)을 유지하면서도, 요청하신 RAG API 계약을 최소 변경으로 수용하는 기준안이다.
