package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.dto.response.OcrTriggerResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.ocr.entity.SettlementOcrJob;
import com.matchimban.matchimban_api.settlement.ocr.enums.OcrJobStatus;
import com.matchimban.matchimban_api.settlement.ocr.repository.SettlementOcrJobRepository;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementOcrTriggerService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final SettlementOcrJobRepository ocrJobRepository;

    @Transactional
    public OcrTriggerResponse trigger(Long meetingId, Long memberId) {
        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getRole() != MeetingParticipant.Role.HOST) {
            throw new ApiException(MeetingErrorCode.ONLY_HOST_ALLOWED);
        }
        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }

        MeetingSettlement settlement = meetingSettlementRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ApiException(SettlementErrorCode.RECEIPT_NOT_UPLOADED));

        if (settlement.getReceiptImageUrl() == null || settlement.getReceiptImageUrl().isBlank()) {
            throw new ApiException(SettlementErrorCode.RECEIPT_NOT_UPLOADED);
        }

        if (!(settlement.getSettlementStatus() == SettlementStatus.RECEIPT_UPLOADED
                || settlement.getSettlementStatus() == SettlementStatus.OCR_FAILED)) {
            throw new ApiException(SettlementErrorCode.OCR_NOT_ALLOWED);
        }

        var activeOpt = ocrJobRepository.findFirstBySettlementIdAndStatusInOrderByIdDesc(
                settlement.getId(), List.of(OcrJobStatus.PENDING, OcrJobStatus.PROCESSING));

        if (activeOpt.isPresent()) {
            throw new ApiException(SettlementErrorCode.OCR_ALREADY_IN_PROGRESS);
        }

        String requestId = "req-" + settlement.getId() + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID();

        SettlementOcrJob job = SettlementOcrJob.builder()
                .settlement(settlement)
                .requestId(requestId)
                .status(OcrJobStatus.PENDING)
                .attemptCount(0)
                .build();

        try {
            ocrJobRepository.save(job);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(SettlementErrorCode.OCR_ALREADY_IN_PROGRESS);
        }

        settlement.changeStatus(SettlementStatus.OCR_PROCESSING);

        return new OcrTriggerResponse(settlement.getId(), settlement.getSettlementStatus(), requestId);
    }
}