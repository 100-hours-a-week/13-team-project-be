package com.matchimban.matchimban_api.chat.service.serviceImpl;

import com.matchimban.matchimban_api.chat.config.ChatUploadProperties;
import com.matchimban.matchimban_api.chat.dto.http.ChatImagePresignData;
import com.matchimban.matchimban_api.chat.dto.http.ChatImagePresignRequest;
import com.matchimban.matchimban_api.chat.error.ChatErrorCode;
import com.matchimban.matchimban_api.chat.service.ChatImageUploadService;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatImageUploadServiceImpl implements ChatImageUploadService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final Pattern SAFE_EXTENSION = Pattern.compile("^[a-z0-9]{1,10}$");
	private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 10_485_760L;
	private static final int DEFAULT_PRESIGN_EXPIRES_SECONDS = 120;

	private final MeetingParticipantRepository meetingParticipantRepository;
	private final ChatUploadProperties chatUploadProperties;
	private final S3Presigner s3Presigner;

	@Value("${spring.cloud.aws.region.static:ap-northeast-2}")
	private String awsRegion;

	@Override
	public ChatImagePresignData issuePresignedUpload(
		Long memberId,
		Long meetingId,
		ChatImagePresignRequest request
	) {
		assertActiveParticipant(memberId, meetingId);

		String bucket = normalizedBucket();
		String contentType = normalizeContentType(request.contentType());
		validateContentType(contentType);
		validateFileSize(request.fileSize());

		String extension = resolveFileExtension(request.fileName(), contentType);
		String fileKey = buildFileKey(extension);
		int expiresInSeconds = presignExpiresSeconds();

		try {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(fileKey)
				.contentType(contentType)
				.build();

			PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofSeconds(expiresInSeconds))
				.putObjectRequest(putObjectRequest)
				.build();

			PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

			return new ChatImagePresignData(
				"PUT",
				presigned.url().toString(),
				fileKey,
				buildPublicUrl(bucket, fileKey),
				expiresInSeconds,
				Map.of("Content-Type", contentType)
			);
		} catch (RuntimeException ex) {
			log.error(
				"Failed to issue chat image presign. meetingId={}, memberId={}, bucket={}, contentType={}, fileSize={}, reason={}",
				meetingId,
				memberId,
				bucket,
				contentType,
				request.fileSize(),
				ex.getMessage(),
				ex
			);
			throw new ApiException(ChatErrorCode.CHAT_IMAGE_PRESIGN_FAILED);
		}
	}

	private void assertActiveParticipant(Long memberId, Long meetingId) {
		boolean isActiveParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
			meetingId,
			memberId,
			MeetingParticipant.Status.ACTIVE
		);
		if (!isActiveParticipant) {
			throw new ApiException(ChatErrorCode.FORBIDDEN);
		}
	}

	private String normalizedBucket() {
		String bucket = chatUploadProperties.getBucket();
		if (bucket == null || bucket.isBlank()) {
			throw new ApiException(ChatErrorCode.CHAT_IMAGE_UPLOAD_NOT_CONFIGURED);
		}
		return bucket.trim();
	}

	private String normalizeContentType(String contentType) {
		return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
	}

	private void validateContentType(String contentType) {
		Set<String> allowedContentTypes = chatUploadProperties.getAllowedContentTypes().stream()
			.filter(type -> type != null && !type.isBlank())
			.map(type -> type.toLowerCase(Locale.ROOT))
			.collect(Collectors.toSet());
		if (!allowedContentTypes.contains(contentType)) {
			throw new ApiException(ChatErrorCode.INVALID_IMAGE_CONTENT_TYPE);
		}
	}

	private void validateFileSize(long fileSize) {
		long maxSize = chatUploadProperties.getMaxFileSizeBytes() > 0
			? chatUploadProperties.getMaxFileSizeBytes()
			: DEFAULT_MAX_FILE_SIZE_BYTES;
		if (fileSize <= 0 || fileSize > maxSize) {
			throw new ApiException(ChatErrorCode.INVALID_IMAGE_FILE_SIZE);
		}
	}

	private String resolveFileExtension(String fileName, String contentType) {
		String extensionFromFileName = extractExtension(fileName);
		if (extensionFromFileName != null) {
			return extensionFromFileName;
		}
		return switch (contentType) {
			case "image/jpeg", "image/jpg" -> "jpg";
			case "image/png" -> "png";
			case "image/webp" -> "webp";
			case "image/gif" -> "gif";
			default -> "bin";
		};
	}

	private String extractExtension(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot < 0 || lastDot == fileName.length() - 1) {
			return null;
		}
		String ext = fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT).trim();
		return SAFE_EXTENSION.matcher(ext).matches() ? ext : null;
	}

	private String buildFileKey(String extension) {
		LocalDate now = LocalDate.now(KST);
		return String.format(
			"chat/%d/%02d/%02d/%s.%s",
			now.getYear(),
			now.getMonthValue(),
			now.getDayOfMonth(),
			UUID.randomUUID(),
			extension
		);
	}

	private int presignExpiresSeconds() {
		int configured = chatUploadProperties.getPresignExpiresSeconds();
		return configured > 0 ? configured : DEFAULT_PRESIGN_EXPIRES_SECONDS;
	}

	private String buildPublicUrl(String bucket, String fileKey) {
		String configuredBaseUrl = trimTrailingSlash(chatUploadProperties.getPublicBaseUrl());
		if (configuredBaseUrl != null) {
			return configuredBaseUrl + "/" + fileKey;
		}
		return "https://" + bucket + ".s3." + awsRegion + ".amazonaws.com/" + fileKey;
	}

	private String trimTrailingSlash(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		int end = trimmed.length();
		while (end > 0 && trimmed.charAt(end - 1) == '/') {
			end--;
		}
		return trimmed.substring(0, end);
	}
}
