package com.matchimban.matchimban_api.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메시지 발신자 정보")
public record ChatSenderDto(
	@JsonProperty("user_id")
	@Schema(example = "3")
	Long userId,
	@Schema(example = "김지은")
	String name,
	@JsonProperty("profile_image_url")
	@Schema(example = "https://cdn.example.com/profile/3.png")
	String profileImageUrl
) {
}
