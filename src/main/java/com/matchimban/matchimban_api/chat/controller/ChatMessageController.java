package com.matchimban.matchimban_api.chat.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.chat.dto.http.ChatMessagesLoadedData;
import com.matchimban.matchimban_api.chat.dto.http.ChatReadPointerUpdateRequest;
import com.matchimban.matchimban_api.chat.dto.http.ChatReadPointerUpdatedData;
import com.matchimban.matchimban_api.chat.service.ChatService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.api.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Chat", description = "모임 채팅 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/meetings")
public class ChatMessageController {

	private final ChatService chatService;

	@Operation(
		summary = "메시지 목록 조회 V2",
		description = "모임 내부 단체 톡방 메시지를 cursor 기반으로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "messages_loaded"),
		@ApiResponse(
			responseCode = "401",
			description = "unauthorized",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "403",
			description = "forbidden",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "500",
			description = "internal_server_error",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		)
	})
	@GetMapping("/{meetingId}/messages")
	public ResponseEntity<ApiResult<ChatMessagesLoadedData>> getMessages(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable Long meetingId,
		@RequestParam(required = false) String cursor,
		@RequestParam(defaultValue = "30") @Min(1) @Max(100) int size
	) {
		ChatMessagesLoadedData data = chatService.getMessages(
			principal.memberId(),
			meetingId,
			cursor,
			size
		);
		return ResponseEntity.ok(ApiResult.of("messages_loaded", data));
	}

	@Operation(
		summary = "읽음 포인터 업데이트 V2",
		description = "현재 사용자의 모임 채팅 last_read_id를 전진시킵니다. 같은 값/이전 값이면 no-op입니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "read_pointer_updated"),
		@ApiResponse(
			responseCode = "400",
			description = "invalid_read_pointer",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "401",
			description = "unauthorized",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		),
		@ApiResponse(
			responseCode = "403",
			description = "forbidden",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		)
	})
	@PostMapping("/{meetingId}/read-pointer")
	public ResponseEntity<ApiResult<ChatReadPointerUpdatedData>> updateReadPointer(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable Long meetingId,
		@Validated @RequestBody ChatReadPointerUpdateRequest request
	) {
		ChatReadPointerUpdatedData data = chatService.updateReadPointer(
			principal.memberId(),
			meetingId,
			request.lastReadMessageId()
		);
		return ResponseEntity.ok(ApiResult.of("read_pointer_updated", data));
	}
}
