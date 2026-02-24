# Chat V2 Backend 문서

## 범위
- 포함: 메시지 영구 저장(RDB), 실시간 전파(STOMP), Redis Pub/Sub 서버 간 브로드캐스트, heartbeat, `last_read_id` 기반 unread_count
- 제외: 메시지 수정/리액션, 모든 과거 메시지에 대한 상시 정확 실시간 unread_count 보장

## HTTP (Swagger/OpenAPI)
- `GET /api/v2/meetings/{meetingId}/messages`
  - cursor 기반 페이징 (`id DESC`, `size + 1`)
  - 응답: `ApiResult(message="messages_loaded", data=...)`
- `POST /api/v2/meetings/{meetingId}/read-pointer`
  - body: `{ "last_read_message_id": 13003 }`
  - 현재 사용자의 `meeting_participants.last_read_id`를 전진시킴 (`GREATEST` 규칙)
  - 업데이트 시 최근 unread_count 윈도우 재계산/브로드캐스트 트리거

## STOMP (AsyncAPI)
- WebSocket endpoint: `/api/v2/ws`
- SEND
  - `/api/v2/app/meetings/{meetingId}/messages`
  - `/api/v2/app/heartbeat`
- SUBSCRIBE
  - `/api/v2/topic/meetings/{meetingId}/messages`
  - `/api/v2/topic/meetings/{meetingId}/unread-counts`
  - `/user/queue/messages/ack`
  - `/user/queue/heartbeat`
  - `/user/queue/errors`

상세 스펙은 `docs/asyncapi/chat-v2.yaml` 참고.
- Web UI: `http://localhost:8080/asyncapi-ui`
- Raw spec(서버 제공): `http://localhost:8080/asyncapi/chat-v2.yaml`
- STOMP 직접 테스트 UI: `http://localhost:8080/stomp-ui`

## 서버 간 브로드캐스트
- Redis Pub/Sub 채널: `chat.redis.channel.message-created`
- Redis Pub/Sub 채널: `chat.redis.channel.unread-counts-updated`
- 트랜잭션 커밋 후 Redis publish
- 모든 WAS가 동일 채널 subscribe 후 로컬 STOMP topic으로 fan-out

## unread_count 계산 정책 (구현)
- 기준: `ACTIVE` 참여자 중 `last_read_id < message_id` 인원 수
- 송신자 처리: 메시지 저장 트랜잭션에서 송신자 `last_read_id`를 해당 `message_id`까지 즉시 전진
- 재계산 트리거:
  - 메시지 생성 시
  - 읽음 포인터 업데이트 시 (`POST /read-pointer`)
- 브로드캐스트 범위: 최근 `chat.unread.window-size` (기본 150개)
- `server_version`: Redis `INCR(chat:meeting:unread-version:{meetingId})`

## DB 변경
- `chat_messages` 테이블 추가
- 인덱스: `(meeting_id, id)`
- `meetings.last_chat_id`는 `GREATEST(COALESCE(last_chat_id,0), new_message_id)` 방식으로 갱신

## 마이그레이션
- Flyway: `src/main/resources/db/migration/V2__create_chat_messages.sql`
- Flyway: `src/main/resources/db/migration/V3__add_chat_message_client_message_id.sql`
- Flyway: `src/main/resources/db/migration/V4__add_meeting_participant_last_read_id.sql`
- Flyway: `src/main/resources/db/migration/V5__drop_meeting_participant_last_read_at.sql`
- 로컬 기준 스키마: `schema.sql`
