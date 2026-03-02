package com.matchimban.matchimban_api.chat.dto.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "채팅 이미지 Presigned URL 발급 응답 데이터")
public record ChatImagePresignData(
	@JsonProperty("upload_method")
	@Schema(example = "PUT")
	String uploadMethod,
	@JsonProperty("upload_url")
	@Schema(example = "https://s3....")
	String uploadUrl,
	@JsonProperty("file_key")
	@Schema(example = "chat/2026/02/27/abc123.png")
	String fileKey,
	@JsonProperty("public_url")
	@Schema(example = "https://cdn.example.com/chat/2026/02/27/abc123.png")
	String publicUrl,
	@JsonProperty("expires_in_seconds")
	@Schema(example = "120")
	int expiresInSeconds,
	@JsonProperty("required_headers")
	Map<String, String> requiredHeaders
) {
}

