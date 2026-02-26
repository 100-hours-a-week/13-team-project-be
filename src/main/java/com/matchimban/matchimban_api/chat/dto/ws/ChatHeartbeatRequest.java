package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "헬스체크 요청")
public record ChatHeartbeatRequest(
	@Schema(example = "HEARTBEAT")
	String type,
	@JsonProperty("client_time")
	@Schema(nullable = true, example = "2026-01-08T15:11:00+09:00")
	OffsetDateTime clientTime
) {
}
