package com.matchimban.matchimban_api.chat.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chat.upload")
public class ChatUploadProperties {

	private String bucket = "";
	private String publicBaseUrl = "";
	private int presignExpiresSeconds = 120;
	private long maxFileSizeBytes = 10_485_760L;
	private List<String> allowedContentTypes = List.of(
		"image/jpeg",
		"image/jpg",
		"image/png",
		"image/webp",
		"image/gif"
	);

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getPublicBaseUrl() {
		return publicBaseUrl;
	}

	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}

	public int getPresignExpiresSeconds() {
		return presignExpiresSeconds;
	}

	public void setPresignExpiresSeconds(int presignExpiresSeconds) {
		this.presignExpiresSeconds = presignExpiresSeconds;
	}

	public long getMaxFileSizeBytes() {
		return maxFileSizeBytes;
	}

	public void setMaxFileSizeBytes(long maxFileSizeBytes) {
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	public List<String> getAllowedContentTypes() {
		return allowedContentTypes;
	}

	public void setAllowedContentTypes(List<String> allowedContentTypes) {
		this.allowedContentTypes = allowedContentTypes;
	}
}

