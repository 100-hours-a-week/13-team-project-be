package com.matchimban.matchimban_api.chat.dto.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "읽음 포인터 업데이트 요청")
public record ChatReadPointerUpdateRequest(
	@JsonProperty("last_read_message_id")
	@NotNull
	@Positive
	@Schema(example = "13003")
	Long lastReadMessageId
) {
}
