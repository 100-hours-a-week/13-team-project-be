package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementProgressResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.ocr.entity.SettlementOcrJob;
import com.matchimban.matchimban_api.settlement.ocr.enums.OcrJobStatus;
import com.matchimban.matchimban_api.settlement.ocr.repository.SettlementOcrJobRepository;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementProgressService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final SettlementOcrJobRepository ocrJobRepository;

    public SettlementProgressResponse getProgress(Long meetingId, Long memberId) {

        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }

        MeetingSettlement settlement = meetingSettlementRepository.findByMeetingId(meetingId).orElse(null);

        if (settlement == null) {
            return new SettlementProgressResponse(
                    null,
                    SettlementStatus.NOT_STARTED,
                    null,
                    null
            );
        }

        SettlementStatus status = settlement.getSettlementStatus();
        Long settlementId = settlement.getId();

        String requestId = null;
        SettlementProgressResponse.OcrError error = null;

        if (status == SettlementStatus.OCR_PROCESSING) {
            requestId = ocrJobRepository
                    .findFirstBySettlementIdAndStatusInOrderByIdDesc(
                            settlementId,
                            List.of(OcrJobStatus.PENDING, OcrJobStatus.PROCESSING)
                    )
                    .map(SettlementOcrJob::getRequestId)
                    .orElseGet(() -> ocrJobRepository.findTopBySettlementIdOrderByIdDesc(settlementId)
                            .map(SettlementOcrJob::getRequestId)
                            .orElse(null));
        }

        if (status == SettlementStatus.OCR_FAILED) {
            Optional<SettlementOcrJob> failedJobOpt =
                    ocrJobRepository.findTopBySettlementIdAndStatusOrderByIdDesc(settlementId, OcrJobStatus.FAILED);

            if (failedJobOpt.isPresent()) {
                SettlementOcrJob job = failedJobOpt.get();
                if (job.getLastErrorCode() != null || job.getLastErrorMessage() != null) {
                    error = new SettlementProgressResponse.OcrError(
                            safe(job.getLastErrorCode(), "OCR_FAILED"),
                            safe(job.getLastErrorMessage(), "영수증 인식에 실패했어요.")
                    );
                }
                requestId = job.getRequestId();
            }
        }

        return new SettlementProgressResponse(
                settlementId,
                status,
                requestId,
                error
        );
    }

    private String safe(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}