package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementStateResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.SettlementParticipant;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import com.matchimban.matchimban_api.settlement.enums.SettlementNextAction;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.SettlementParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementStateService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;

    public SettlementStateResponse getState(Long meetingId, Long memberId) {

        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }

        boolean isHost = mp.getRole() == MeetingParticipant.Role.HOST;

        MeetingSettlement settlement = meetingSettlementRepository.findByMeetingId(meetingId).orElse(null);

        SettlementStatus settlementStatus = (settlement == null)
                ? SettlementStatus.NOT_STARTED
                : settlement.getSettlementStatus();

        Long settlementId = (settlement == null) ? null : settlement.getId();

        boolean mySelectionConfirmed = false;
        PaymentStatus myPaymentStatus = null;

        if (settlement != null) {
            Optional<SettlementParticipant> spOpt =
                    settlementParticipantRepository.findBySettlementIdAndParticipantId(settlement.getId(), mp.getId());

            if (spOpt.isPresent()) {
                SettlementParticipant sp = spOpt.get();
                mySelectionConfirmed = sp.isSelectionConfirmed();
                myPaymentStatus = sp.getPaymentStatus();
            }
        }

        SettlementNextAction nextAction = decideNextAction(isHost, settlementStatus, mySelectionConfirmed, myPaymentStatus);

        String message = null;
        if (!isHost && nextAction == SettlementNextAction.GO_MEETING_DETAIL_WITH_MODAL) {
            message = "정산이 아직 시작되지 않았어요. 모임장이 영수증을 확정하면 참여할 수 있어요.";
        }

        return new SettlementStateResponse(
                settlementId,
                settlementStatus,
                isHost ? "HOST" : "MEMBER",
                nextAction,
                message
        );
    }

    private SettlementNextAction decideNextAction(
            boolean isHost,
            SettlementStatus status,
            boolean mySelectionConfirmed,
            PaymentStatus myPaymentStatus
    ) {
        if (isHost) {
            return switch (status) {
                case NOT_STARTED, RECEIPT_UPLOADED -> SettlementNextAction.GO_RECEIPT_UPLOAD;
                case OCR_PROCESSING -> SettlementNextAction.GO_OCR_LOADING;
                case OCR_FAILED -> SettlementNextAction.GO_OCR_FAILED;
                case OCR_SUCCEEDED -> SettlementNextAction.GO_OCR_EDIT;
                case SELECTION_OPEN -> SettlementNextAction.GO_MENU_SELECTION;
                case CALCULATING -> SettlementNextAction.GO_WAITING;
                case RESULT_READY -> SettlementNextAction.GO_RESULT;
                case COMPLETED -> SettlementNextAction.GO_COMPLETED;
            };
        }

        return switch (status) {
            case NOT_STARTED, RECEIPT_UPLOADED, OCR_PROCESSING, OCR_FAILED, OCR_SUCCEEDED ->
                    SettlementNextAction.GO_MEETING_DETAIL_WITH_MODAL;

            case SELECTION_OPEN ->
                    mySelectionConfirmed ? SettlementNextAction.GO_WAITING : SettlementNextAction.GO_MENU_SELECTION;

            case CALCULATING ->
                    SettlementNextAction.GO_WAITING;

            case RESULT_READY -> {
                if (myPaymentStatus == PaymentStatus.DONE) yield SettlementNextAction.GO_COMPLETED;
                yield SettlementNextAction.GO_RESULT;
            }

            case COMPLETED ->
                    SettlementNextAction.GO_COMPLETED;
        };
    }
}