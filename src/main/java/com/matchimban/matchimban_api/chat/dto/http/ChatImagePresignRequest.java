package com.matchimban.matchimban_api.chat.dto.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅 이미지 Presigned URL 발급 요청")
public record ChatImagePresignRequest(
	@JsonProperty("content_type")
	@NotBlank
	@Size(max = 100)
	@Schema(example = "image/png")
	String contentType,
	@JsonProperty("file_name")
	@NotBlank
	@Size(max = 255)
	@Schema(example = "photo.png")
	String fileName,
	@JsonProperty("file_size")
	@Positive
	@Schema(example = "345678")
	long fileSize
) {
}

