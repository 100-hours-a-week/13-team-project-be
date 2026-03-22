package com.matchimban.matchimban_api.ragchat.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.api.ErrorResponse;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import com.matchimban.matchimban_api.ragchat.dto.request.RagChatAskRequest;
import com.matchimban.matchimban_api.ragchat.dto.response.RagChatAskData;
import com.matchimban.matchimban_api.ragchat.dto.response.RagHealthData;
import com.matchimban.matchimban_api.ragchat.dto.response.RagHistoryData;
import com.matchimban.matchimban_api.ragchat.dto.queue.RagChatQueueMessage;
import com.matchimban.matchimban_api.ragchat.queue.RagChatQueuePublisher;
import com.matchimban.matchimban_api.ragchat.service.RagChatService;
import com.matchimban.matchimban_api.ragchat.sse.RagChatSseRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@Tag(name = "RagChat", description = "RAG 챗봇 API")
@RestController
@RequiredArgsConstructor
public class RagChatController {

	private final RagChatService ragChatService;
	private final RagChatSseRegistry sseRegistry;
	private final RagChatQueuePublisher queuePublisher;

	@CsrfRequired
	@Operation(summary = "RAG 질문", description = "질문을 전송하고 식당 추천 답변을 받습니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "rag_chat_answered"),
		@ApiResponse(
			responseCode = "400",
			description = "validation_failed / rag_engine_bad_request",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "403",
			description = "forbidden_user_id",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "500",
			description = "rag_engine_failed",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		)
	})
	@PostMapping({"/api/v1/rag-chat", "/RAGchat"})
	public ResponseEntity<ApiResult<RagChatAskData>> ask(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @RequestBody RagChatAskRequest request
	) {
		RagChatAskData data = ragChatService.ask(principal.memberId(), request);
		return ResponseEntity.ok(ApiResult.of("rag_chat_answered", data));
	}

	@CsrfRequired
	@Operation(summary = "RAG 대화 초기화", description = "user_id 기준 대화 기록을 초기화합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "rag_chat_history_cleared"),
		@ApiResponse(
			responseCode = "400",
			description = "validation_failed / rag_engine_bad_request",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "403",
			description = "forbidden_user_id",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		)
	})
	@DeleteMapping({"/api/v1/rag-chat/{user_id}", "/RAGchat/{user_id}"})
	public ResponseEntity<ApiResult<Void>> resetHistory(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable("user_id") @NotBlank String userId
	) {
		ragChatService.resetHistory(principal.memberId(), userId);
		return ResponseEntity.ok(ApiResult.of("rag_chat_history_cleared"));
	}

	@Operation(summary = "RAG 대화 이력 조회", description = "채팅 화면 진입 및 위로 스크롤 시 과거 대화를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "rag_history_loaded"),
		@ApiResponse(
			responseCode = "400",
			description = "validation_failed / rag_engine_bad_request",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "403",
			description = "forbidden_user_id",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		)
	})
	@GetMapping({"/api/v1/rag-chat/history/{user_id}", "/history/{user_id}"})
	public ResponseEntity<ApiResult<RagHistoryData>> getHistory(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable("user_id") @NotBlank String userId,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
		@RequestParam(name = "before_id", required = false) @Positive Long beforeId
	) {
		RagHistoryData data = ragChatService.getHistory(principal.memberId(), userId, limit, beforeId);
		return ResponseEntity.ok(ApiResult.of("rag_history_loaded", data));
	}

	@CsrfRequired
	@Operation(summary = "RAG 질문 (SSE 스트림)", description = "질문을 큐에 넣고 SSE로 결과를 스트리밍합니다.")
	@PostMapping(value = "/api/v1/rag-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamAsk(
		@AuthenticationPrincipal MemberPrincipal principal,
		@Valid @RequestBody RagChatAskRequest request
	) {
		ragChatService.assertOwner(principal.memberId(), request.userId());

		String requestId = UUID.randomUUID().toString();
		SseEmitter emitter = new SseEmitter(60_000L);
		sseRegistry.register(requestId, emitter);

		queuePublisher.publish(new RagChatQueueMessage(requestId, request.userId(), request.message()));

		return emitter;
	}

	@Operation(summary = "RAG 서버 상태 확인")
	@ApiResponse(responseCode = "200", description = "rag_chat_health_ok")
	@GetMapping({"/api/v1/rag-chat/health", "/health"})
	public ResponseEntity<ApiResult<RagHealthData>> health() {
		RagHealthData data = ragChatService.health();
		return ResponseEntity.ok(ApiResult.of("rag_chat_health_ok", data));
	}
}
