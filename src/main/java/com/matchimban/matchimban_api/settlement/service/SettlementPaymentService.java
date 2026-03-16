package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.event.NotificationRequestedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementPaymentService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

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
            publishSettlementPaymentRequestedNotification(meetingId, mp.getMeeting().getHostMemberId(), sp.getId());
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
            publishSettlementPaymentConfirmedNotification(
                    meetingId,
                    sp.getParticipant().getMember().getId(),
                    sp.getParticipant().getId()
            );
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

        List<Long> recipients = settlementParticipantRepository.findAllBySettlementId(settlement.getId()).stream()
                .filter(participant -> participant.getPaymentStatus() == PaymentStatus.UNPAID)
                .map(participant -> participant.getParticipant().getMember().getId())
                .toList();
        publishSettlementUnpaidRemindNotification(meetingId, recipients);

        return new RemindUnpaidResponse(count);
    }

    private void publishSettlementPaymentRequestedNotification(Long meetingId, Long hostMemberId, Long settlementParticipantId) {
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                NotificationType.SETTLEMENT_PAYMENT_REQUESTED,
                "송금 확인 요청",
                "송금 확인 요청이 도착했어요.",
                "SETTLEMENT",
                meetingId,
                settlementParticipantId,
                "/meetings/" + meetingId + "/settlement/result",
                "SETTLEMENT_PAYMENT_REQUESTED:" + meetingId + ":" + settlementParticipantId,
                null,
                List.of(hostMemberId)
        ));
    }

    private void publishSettlementPaymentConfirmedNotification(Long meetingId, Long recipientMemberId, Long meetingParticipantId) {
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                NotificationType.SETTLEMENT_PAYMENT_CONFIRMED,
                "송금 확인 완료",
                "송금이 확인되었어요.",
                "SETTLEMENT",
                meetingId,
                meetingParticipantId,
                "/meetings/" + meetingId + "/settlement/result",
                "SETTLEMENT_PAYMENT_CONFIRMED:" + meetingId + ":" + meetingParticipantId,
                null,
                List.of(recipientMemberId)
        ));
    }

    private void publishSettlementUnpaidRemindNotification(Long meetingId, List<Long> recipients) {
        if (recipients.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                NotificationType.SETTLEMENT_UNPAID_REMIND,
                "미송금 안내",
                "아직 송금이 완료되지 않았어요. 송금을 진행해 주세요.",
                "SETTLEMENT",
                meetingId,
                null,
                "/meetings/" + meetingId + "/settlement/result",
                "SETTLEMENT_UNPAID_REMIND:" + meetingId + ":" + Instant.now().toEpochMilli(),
                null,
                recipients
        ));
    }
}
