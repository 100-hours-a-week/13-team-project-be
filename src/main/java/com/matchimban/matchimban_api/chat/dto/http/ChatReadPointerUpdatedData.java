package com.matchimban.matchimban_api.chat.dto.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽음 포인터 업데이트 결과")
public record ChatReadPointerUpdatedData(
	@JsonProperty("meeting_id")
	@Schema(example = "10")
	Long meetingId,
	@JsonProperty("last_read_message_id")
	@Schema(example = "6650a1b2c3d4e5f678901234")
	String lastReadMessageId,
	@Schema(description = "포인터가 실제로 전진했는지 여부", example = "true")
	boolean updated
) {
}
