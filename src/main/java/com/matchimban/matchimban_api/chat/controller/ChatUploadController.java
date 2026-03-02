package com.matchimban.matchimban_api.chat.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.chat.dto.http.ChatImagePresignData;
import com.matchimban.matchimban_api.chat.dto.http.ChatImagePresignRequest;
import com.matchimban.matchimban_api.chat.service.ChatImageUploadService;
import com.matchimban.matchimban_api.global.dto.ApiResult;
import com.matchimban.matchimban_api.global.error.api.ErrorResponse;
import com.matchimban.matchimban_api.global.swagger.CsrfRequired;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "모임 채팅 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/meetings")
public class ChatUploadController {

	private final ChatImageUploadService chatImageUploadService;

	@CsrfRequired
	@Operation(
		summary = "채팅 이미지 업로드 URL 발급 V2",
		description = "현재 사용자가 모임 채팅용 이미지를 업로드할 수 있는 Presigned PUT URL을 발급합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "chat_image_upload_url_issued"),
		@ApiResponse(
			responseCode = "400",
			description = "invalid_image_content_type / invalid_image_file_size",
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
		),
		@ApiResponse(
			responseCode = "500",
			description = "chat_image_upload_not_configured / chat_image_presign_failed",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class))
		)
	})
	@PostMapping("/{meetingId}/images/presign")
	public ResponseEntity<ApiResult<ChatImagePresignData>> issueChatImagePresign(
		@AuthenticationPrincipal MemberPrincipal principal,
		@PathVariable Long meetingId,
		@Valid @RequestBody ChatImagePresignRequest request
	) {
		ChatImagePresignData data = chatImageUploadService.issuePresignedUpload(
			principal.memberId(),
			meetingId,
			request
		);
		return ResponseEntity.ok(ApiResult.of("chat_image_upload_url_issued", data));
	}
}

