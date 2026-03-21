package com.matchimban.matchimban_api.chat.dto.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "읽음 포인터 업데이트 요청")
public record ChatReadPointerUpdateRequest(
	@JsonProperty("last_read_message_id")
	@NotBlank
	@Schema(example = "6650a1b2c3d4e5f678901234")
	String lastReadMessageId
) {
}
