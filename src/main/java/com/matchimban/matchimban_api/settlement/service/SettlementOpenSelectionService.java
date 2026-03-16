package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.event.NotificationRequestedEvent;
import com.matchimban.matchimban_api.settlement.dto.request.OpenSelectionRequest;
import com.matchimban.matchimban_api.settlement.dto.response.OpenSelectionResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.ReceiptItem;
import com.matchimban.matchimban_api.settlement.entity.SettlementParticipant;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.ReceiptItemRepository;
import com.matchimban.matchimban_api.settlement.repository.SettlementParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementOpenSelectionService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OpenSelectionResponse open(Long meetingId, Long memberId, OpenSelectionRequest request) {

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

        if (settlement.getSettlementStatus() == SettlementStatus.SELECTION_OPEN) {
            int initialized = settlementParticipantRepository.findAllBySettlementId(settlement.getId()).size();
            return new OpenSelectionResponse(settlement.getId(), settlement.getSettlementStatus(), initialized);
        }

        if (settlement.getSettlementStatus() != SettlementStatus.OCR_SUCCEEDED) {
            throw new ApiException(SettlementErrorCode.OPEN_SELECTION_NOT_ALLOWED);
        }

        settlement.applyOcrSummary(request.totalAmount(), request.discountAmount());

        applyReceiptItems(settlement, request.items());

        int created = initSettlementParticipants(settlement, meetingId);

        settlement.changeStatus(SettlementStatus.SELECTION_OPEN);
        publishSettlementSelectionOpenNotification(meetingId, settlement.getId());

        return new OpenSelectionResponse(settlement.getId(), settlement.getSettlementStatus(), created);
    }

    private void applyReceiptItems(MeetingSettlement settlement, List<OpenSelectionRequest.Item> items) {

        Long settlementId = settlement.getId();
        List<ReceiptItem> existing = receiptItemRepository.findAllBySettlementIdOrderByIdAsc(settlementId);

        Map<Long, ReceiptItem> existingById = existing.stream()
                .filter(it -> it.getId() != null)
                .collect(Collectors.toMap(ReceiptItem::getId, it -> it));

        boolean allIncomingIdsNull = items.stream().allMatch(i -> i.itemId() == null);

        if (allIncomingIdsNull) {
            receiptItemRepository.deleteBySettlementId(settlementId);
            for (OpenSelectionRequest.Item in : items) {
                receiptItemRepository.save(ReceiptItem.builder()
                        .settlement(settlement)
                        .itemName(in.name())
                        .unitPrice(in.unitPrice())
                        .quantity(in.quantity())
                        .totalPrice(in.totalPrice())
                        .build());
            }
            return;
        }

        Set<Long> keepIds = new HashSet<>();
        for (OpenSelectionRequest.Item in : items) {
            if (in.itemId() != null) {
                ReceiptItem target = existingById.get(in.itemId());
                if (target == null) {
                    throw new ApiException(SettlementErrorCode.INVALID_RECEIPT_ITEM_ID);
                }
                target.update(in.name(), in.unitPrice(), in.quantity(), in.totalPrice());
                keepIds.add(in.itemId());
            } else {
                receiptItemRepository.save(ReceiptItem.builder()
                        .settlement(settlement)
                        .itemName(in.name())
                        .unitPrice(in.unitPrice())
                        .quantity(in.quantity())
                        .totalPrice(in.totalPrice())
                        .build());
            }
        }

        List<Long> toDeleteIds = existing.stream()
                .map(ReceiptItem::getId)
                .filter(Objects::nonNull)
                .filter(id -> !keepIds.contains(id))
                .toList();

        if (!toDeleteIds.isEmpty()) {
            receiptItemRepository.deleteAllByIdInBatch(toDeleteIds);
        }
    }

    private int initSettlementParticipants(MeetingSettlement settlement, Long meetingId) {

        List<MeetingParticipant> activeParticipants = meetingParticipantRepository.findActiveParticipants(meetingId);

        Set<Long> existingParticipantIds = settlementParticipantRepository.findAllBySettlementId(settlement.getId())
                .stream()
                .map(sp -> sp.getParticipant().getId())
                .collect(Collectors.toSet());

        int created = 0;

        for (MeetingParticipant mp : activeParticipants) {
            if (existingParticipantIds.contains(mp.getId())) continue;

            SettlementParticipant sp = SettlementParticipant.builder()
                    .settlement(settlement)
                    .participant(mp)
                    .paymentStatus(PaymentStatus.UNPAID)
                    .build();

            try {
                settlementParticipantRepository.save(sp);
                created++;
            } catch (DataIntegrityViolationException ignored) {
            }
        }

        return created;
    }

    private void publishSettlementSelectionOpenNotification(Long meetingId, Long settlementId) {
        List<Long> recipients = meetingParticipantRepository.findActiveParticipants(meetingId).stream()
                .filter(participant -> participant.getRole() == MeetingParticipant.Role.MEMBER)
                .map(participant -> participant.getMember().getId())
                .toList();

        if (recipients.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                NotificationType.SETTLEMENT_SELECTION_OPEN,
                "정산 메뉴 확인 요청",
                "정산할 메뉴를 선택해 주세요.",
                "SETTLEMENT",
                meetingId,
                settlementId,
                "/meetings/" + meetingId + "/settlement/selection",
                "SETTLEMENT_SELECTION_OPEN:" + meetingId + ":" + settlementId,
                null,
                recipients
        ));
    }
}
