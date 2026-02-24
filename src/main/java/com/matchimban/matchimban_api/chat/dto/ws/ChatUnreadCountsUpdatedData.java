package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "unread_count 브로드캐스트 본문")
public record ChatUnreadCountsUpdatedData(
	@JsonProperty("meeting_id")
	@Schema(example = "10")
	Long meetingId,
	ChatUnreadCountsBasis basis,
	List<ChatUnreadCountItem> items,
	@JsonProperty("server_version")
	@Schema(example = "1002")
	long serverVersion,
	@JsonProperty("generated_at")
	@Schema(example = "2026-01-08T15:10:30+09:00")
	OffsetDateTime generatedAt
) {
}
