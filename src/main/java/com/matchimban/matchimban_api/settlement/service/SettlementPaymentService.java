package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.dto.response.PaymentStatusResponse;
import com.matchimban.matchimban_api.settlement.dto.response.RemindUnpaidResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.SettlementParticipant;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.SettlementParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SettlementPaymentService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;

    @Transactional
    public PaymentStatusResponse requestMyPayment(Long meetingId, Long memberId) {
        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }
        if (mp.getRole() != MeetingParticipant.Role.MEMBER) {
            throw new ApiException(SettlementErrorCode.ONLY_MEMBER_ALLOWED);
        }

        MeetingSettlement settlement = meetingSettlementRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ApiException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.getSettlementStatus() != SettlementStatus.RESULT_READY) {
            throw new ApiException(SettlementErrorCode.PAYMENT_NOT_ALLOWED);
        }

        SettlementParticipant sp = settlementParticipantRepository
                .findBySettlementIdAndParticipantId(settlement.getId(), mp.getId())
                .orElseThrow(() -> new ApiException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

        if (sp.getPaymentStatus() == PaymentStatus.UNPAID) {
            sp.requestPayment(Instant.now());
            // TODO(notification): 송금 확인 요청 알림. recipients: HOST MeetingParticipant.memberId
        }

        return new PaymentStatusResponse(settlement.getId(), mp.getId(), sp.getPaymentStatus());
    }

    @Transactional
    public PaymentStatusResponse confirmPayment(Long meetingId, Long memberId, Long targetMeetingParticipantId) {
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

        if (settlement.getSettlementStatus() != SettlementStatus.RESULT_READY) {
            throw new ApiException(SettlementErrorCode.PAYMENT_NOT_ALLOWED);
        }

        SettlementParticipant sp = settlementParticipantRepository
                .findBySettlementIdAndParticipantId(settlement.getId(), targetMeetingParticipantId)
                .orElseThrow(() -> new ApiException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

        if (sp.getPaymentStatus() != PaymentStatus.DONE) {
            sp.confirmPayment(Instant.now());
            // TODO(notification): 송금 완료 확인 알림. recipients: target SettlementParticipant.participant.memberId
        }

        return new PaymentStatusResponse(settlement.getId(), targetMeetingParticipantId, sp.getPaymentStatus());
    }

    @Transactional(readOnly = true)
    public RemindUnpaidResponse remindUnpaid(Long meetingId, Long memberId) {
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

        if (settlement.getSettlementStatus() != SettlementStatus.RESULT_READY) {
            throw new ApiException(SettlementErrorCode.PAYMENT_NOT_ALLOWED);
        }

        long count = settlementParticipantRepository.countBySettlementIdAndPaymentStatus(settlement.getId(), PaymentStatus.UNPAID);

        // TODO(notification): 미송금자 송금 요청 알림. recipients: PaymentStatus.UNPAID SettlementParticipant.participant.memberId
        return new RemindUnpaidResponse(count);
    }
}
