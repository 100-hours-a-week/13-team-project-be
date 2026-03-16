package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.event.NotificationRequestedEvent;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementCompleteResponse;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementCompletedResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.SettlementParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementCompleteService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SettlementCompleteResponse complete(Long meetingId, Long memberId) {
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
                .orElseThrow(() -> new ApiException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.getSettlementStatus() == SettlementStatus.COMPLETED) {
            return new SettlementCompleteResponse(settlement.getId(), settlement.getSettlementStatus());
        }

        if (settlement.getSettlementStatus() != SettlementStatus.RESULT_READY) {
            throw new ApiException(SettlementErrorCode.COMPLETE_NOT_ALLOWED);
        }

        List<Long> recipients = settlementParticipantRepository.findAllBySettlementId(settlement.getId()).stream()
                .filter(participant -> participant.getPaymentStatus() != PaymentStatus.DONE)
                .map(participant -> participant.getParticipant().getMember().getId())
                .toList();

        settlementParticipantRepository.bulkSetPaymentDone(settlement.getId(), PaymentStatus.DONE, Instant.now());
        publishSettlementBulkDoneNotification(meetingId, settlement.getId(), recipients);

        meetingSettlementRepository.updateStatusIfCurrent(settlement.getId(), SettlementStatus.RESULT_READY, SettlementStatus.COMPLETED);

        SettlementStatus latest = meetingSettlementRepository.findById(settlement.getId())
                .map(MeetingSettlement::getSettlementStatus)
                .orElse(settlement.getSettlementStatus());

        return new SettlementCompleteResponse(settlement.getId(), latest);
    }

    @Transactional(readOnly = true)
    public SettlementCompletedResponse getCompleted(Long meetingId, Long memberId) {
        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }

        MeetingSettlement settlement = meetingSettlementRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ApiException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.getSettlementStatus() != SettlementStatus.COMPLETED) {
            throw new ApiException(SettlementErrorCode.COMPLETE_NOT_ALLOWED);
        }

        return new SettlementCompletedResponse(settlement.getId(), settlement.getSettlementStatus());
    }

    private void publishSettlementBulkDoneNotification(Long meetingId, Long settlementId, List<Long> recipients) {
        if (recipients.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                NotificationType.SETTLEMENT_BULK_DONE,
                "정산 완료",
                "정산이 모두 완료되었어요.",
                "SETTLEMENT",
                meetingId,
                settlementId,
                "/meetings/" + meetingId + "/settlement/completed",
                "SETTLEMENT_BULK_DONE:" + meetingId + ":" + settlementId,
                null,
                recipients
        ));
    }
}
