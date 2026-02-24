package com.matchimban.matchimban_api.chat.dto.ws;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "헬스체크 ACK")
public record ChatHeartbeatAckMessage(
	@Schema(example = "HEARTBEAT_ACK")
	String type,
	@JsonProperty("server_time")
	@Schema(example = "2026-01-08T15:11:00+09:00")
	OffsetDateTime serverTime
) {
	public static ChatHeartbeatAckMessage now(OffsetDateTime serverTime) {
		return new ChatHeartbeatAckMessage("HEARTBEAT_ACK", serverTime);
	}
}
