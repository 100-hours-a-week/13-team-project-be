package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.config.AppS3Properties;
import com.matchimban.matchimban_api.settlement.dto.request.ReceiptUploadUrlRequest;
import com.matchimban.matchimban_api.settlement.dto.response.ReceiptUploadUrlResponse;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementReceiptUploadService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final S3Presigner s3Presigner;
    private final AppS3Properties appS3Properties;

    public ReceiptUploadUrlResponse createUploadUrl(Long meetingId, Long memberId, ReceiptUploadUrlRequest request) {

        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getRole() != MeetingParticipant.Role.HOST) {
            throw new ApiException(MeetingErrorCode.ONLY_HOST_ALLOWED);
        }

        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }

        String contentType = request.contentType();
        String ext = toExtension(contentType);

        String objectKey = buildReceiptObjectKey(meetingId, ext);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(appS3Properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(appS3Properties.getPresignUploadTtl())
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        Instant expiresAt = Instant.now().plus(appS3Properties.getPresignUploadTtl());
        return new ReceiptUploadUrlResponse(objectKey, presigned.url().toString(), expiresAt);
    }

    private String toExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new ApiException(SettlementErrorCode.UNSUPPORTED_RECEIPT_CONTENT_TYPE);
        };
    }

    private String buildReceiptObjectKey(Long meetingId, String ext) {
        String prefix = appS3Properties.getReceiptPrefix();
        String fileName = "receipt-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ext;
        return prefix + "/meeting-" + meetingId + "/" + fileName;
    }
}