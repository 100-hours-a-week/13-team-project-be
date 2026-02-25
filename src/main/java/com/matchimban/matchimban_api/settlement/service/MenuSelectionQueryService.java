package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.dto.response.MenuSelectionResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.SettlementParticipant;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.ReceiptItemRepository;
import com.matchimban.matchimban_api.settlement.repository.SettlementItemSelectionRepository;
import com.matchimban.matchimban_api.settlement.repository.SettlementParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuSelectionQueryService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;
    private final SettlementItemSelectionRepository selectionRepository;

    public MenuSelectionResponse getSelection(Long meetingId, Long memberId) {

        MeetingParticipant mp = meetingParticipantRepository
                .findByMeetingIdAndMemberIdAndStatusWithGraph(meetingId, memberId, MeetingParticipant.Status.ACTIVE)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.NOT_ACTIVE_PARTICIPANT));

        if (mp.getMeeting().isQuickMeeting()) {
            throw new ApiException(SettlementErrorCode.QUICK_MEETING_SETTLEMENT_NOT_SUPPORTED);
        }

        MeetingSettlement settlement = meetingSettlementRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ApiException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.getSettlementStatus() != SettlementStatus.SELECTION_OPEN) {
            throw new ApiException(SettlementErrorCode.SELECTION_NOT_OPEN);
        }

        SettlementParticipant sp = settlementParticipantRepository
                .findBySettlementIdAndParticipantId(settlement.getId(), mp.getId())
                .orElseGet(() -> SettlementParticipant.builder()
                        .settlement(settlement)
                        .participant(mp)
                        .paymentStatus(PaymentStatus.UNPAID)
                        .build()
                );

        boolean confirmed = sp.isSelectionConfirmed();
        List<Long> mySelected = confirmed && sp.getId() != null
                ? selectionRepository.findItemIdsBySettlementParticipantId(sp.getId())
                : List.of();

        var items = receiptItemRepository.findAllBySettlementIdOrderByIdAsc(settlement.getId())
                .stream()
                .map(it -> new MenuSelectionResponse.Item(
                        it.getId(),
                        it.getItemName(),
                        it.getUnitPrice(),
                        it.getQuantity(),
                        it.getTotalPrice()
                ))
                .toList();

        return new MenuSelectionResponse(
                settlement.getId(),
                settlement.getSettlementStatus(),
                confirmed,
                mySelected,
                items
        );
    }
}