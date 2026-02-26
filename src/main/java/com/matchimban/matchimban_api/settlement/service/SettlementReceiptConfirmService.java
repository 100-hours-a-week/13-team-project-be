package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.config.AppS3Properties;
import com.matchimban.matchimban_api.settlement.dto.request.ReceiptConfirmRequest;
import com.matchimban.matchimban_api.settlement.dto.response.ReceiptConfirmResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementReceiptConfirmService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final AppS3Properties appS3Properties;

    @Transactional
    public ReceiptConfirmResponse confirm(Long meetingId, Long memberId, ReceiptConfirmRequest request) {

        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getRole() != MeetingParticipant.Role.HOST) {
            throw new ApiException(MeetingErrorCode.ONLY_HOST_ALLOWED);
        }

        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }

        String objectKey = request.objectKey();
        validateObjectKey(meetingId, objectKey);

        MeetingSettlement settlement = meetingSettlementRepository.findByMeetingId(meetingId).orElse(null);

        if (settlement == null) {
            try {
                MeetingSettlement created = MeetingSettlement.builder()
                        .meeting(mp.getMeeting())
                        .receiptImageUrl(objectKey)
                        .settlementStatus(SettlementStatus.RECEIPT_UPLOADED)
                        .build();

                MeetingSettlement saved = meetingSettlementRepository.save(created);
                return new ReceiptConfirmResponse(saved.getId(), saved.getSettlementStatus());

            } catch (DataIntegrityViolationException e) {
                settlement = meetingSettlementRepository.findByMeetingId(meetingId)
                        .orElseThrow(() -> e);
            }
        }

        if (!isReceiptConfirmAllowed(settlement.getSettlementStatus())) {
            throw new ApiException(SettlementErrorCode.RECEIPT_CONFIRM_NOT_ALLOWED);
        }

        if (objectKey.equals(settlement.getReceiptImageUrl())
                && settlement.getSettlementStatus() == SettlementStatus.RECEIPT_UPLOADED) {
            return new ReceiptConfirmResponse(settlement.getId(), settlement.getSettlementStatus());
        }

        settlement.attachReceiptImageUrl(objectKey);
        settlement.changeStatus(SettlementStatus.RECEIPT_UPLOADED);

        return new ReceiptConfirmResponse(settlement.getId(), settlement.getSettlementStatus());
    }

    private boolean isReceiptConfirmAllowed(SettlementStatus status) {
        return switch (status) {
            case NOT_STARTED, RECEIPT_UPLOADED, OCR_FAILED -> true;
            case OCR_PROCESSING, OCR_SUCCEEDED, SELECTION_OPEN, CALCULATING, RESULT_READY, COMPLETED -> false;
        };
    }

    private void validateObjectKey(Long meetingId, String objectKey) {
        String requiredPrefix = appS3Properties.getReceiptPrefix() + "/meeting-" + meetingId + "/";
        if (objectKey == null || objectKey.isBlank() || !objectKey.startsWith(requiredPrefix)) {
            throw new ApiException(SettlementErrorCode.INVALID_RECEIPT_OBJECT_KEY);
        }

        String lower = objectKey.toLowerCase();
        if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg"))) {
            throw new ApiException(SettlementErrorCode.UNSUPPORTED_RECEIPT_CONTENT_TYPE);
        }

        if (lower.contains("..")) {
            throw new ApiException(SettlementErrorCode.INVALID_RECEIPT_OBJECT_KEY);
        }
    }
}