package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.event.NotificationRequestedEvent;
import com.matchimban.matchimban_api.settlement.dto.request.MenuSelectionConfirmRequest;
import com.matchimban.matchimban_api.settlement.dto.response.MenuSelectionConfirmResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.ReceiptItem;
import com.matchimban.matchimban_api.settlement.entity.SettlementItemSelection;
import com.matchimban.matchimban_api.settlement.entity.SettlementParticipant;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuSelectionConfirmService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final SettlementParticipantRepository settlementParticipantRepository;
    private final SettlementItemSelectionRepository selectionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MenuSelectionConfirmResponse confirm(Long meetingId, Long memberId, MenuSelectionConfirmRequest request) {

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

        List<Long> rawIds = request.selectedItemIds();
        if (rawIds == null) {
            throw new ApiException(SettlementErrorCode.INVALID_ITEM_ID);
        }
        List<Long> itemIds = rawIds.stream().distinct().toList();

        SettlementParticipant sp = settlementParticipantRepository
                .findBySettlementIdAndParticipantId(settlement.getId(), mp.getId())
                .orElseGet(() -> settlementParticipantRepository.save(
                        SettlementParticipant.builder()
                                .settlement(settlement)
                                .participant(mp)
                                .paymentStatus(PaymentStatus.UNPAID)
                                .build()
                ));

        if (sp.isSelectionConfirmed()) {
            return new MenuSelectionConfirmResponse(settlement.getId(), settlement.getSettlementStatus(), true);
        }

        if (!itemIds.isEmpty()) {
            List<ReceiptItem> receiptItems =
                    receiptItemRepository.findAllBySettlementIdAndIdIn(settlement.getId(), itemIds);

            if (receiptItems.size() != itemIds.size()) {
                throw new ApiException(SettlementErrorCode.INVALID_ITEM_ID);
            }

            selectionRepository.saveAll(
                    receiptItems.stream()
                            .map(item -> SettlementItemSelection.builder()
                                    .settlementParticipant(sp)
                                    .item(item)
                                    .build())
                            .toList()
            );
        }

        sp.markSelectionConfirmed(Instant.now());

        long total = settlementParticipantRepository.countAllBySettlementId(settlement.getId());
        long confirmed = settlementParticipantRepository.countConfirmedBySettlementId(settlement.getId());

        if (confirmed == total) {
            int updated = meetingSettlementRepository.updateStatusIfCurrent(
                    settlement.getId(),
                    SettlementStatus.SELECTION_OPEN,
                    SettlementStatus.CALCULATING
            );
            if (updated == 1) {
                calculateAndPersist(settlement.getId(), meetingId);
                meetingSettlementRepository.updateStatusIfCurrent(
                        settlement.getId(),
                        SettlementStatus.CALCULATING,
                        SettlementStatus.RESULT_READY
                );
                publishSettlementResultReadyNotification(meetingId, settlement.getId());
            }
        }

        SettlementStatus latest = meetingSettlementRepository.findById(settlement.getId())
                .map(MeetingSettlement::getSettlementStatus)
                .orElse(settlement.getSettlementStatus());

        return new MenuSelectionConfirmResponse(settlement.getId(), latest, true);
    }


    private void calculateAndPersist(Long settlementId, Long meetingId) {
        MeetingSettlement settlement = meetingSettlementRepository.findById(settlementId).orElseThrow();

        List<ReceiptItem> items = receiptItemRepository.findAllBySettlementIdOrderByIdAsc(settlementId);
        List<SettlementParticipant> participants = settlementParticipantRepository.findAllBySettlementId(settlementId);

        Long hostSpId = participants.stream()
                .filter(sp -> sp.getParticipant().getRole() == MeetingParticipant.Role.HOST)
                .map(SettlementParticipant::getId)
                .findFirst()
                .orElse(participants.get(0).getId());

        Map<Long, List<Long>> itemIdToSpIds = new HashMap<>();
        for (var row : selectionRepository.findSelectionRowsBySettlementId(settlementId)) {
            itemIdToSpIds.computeIfAbsent(row.getItemId(), k -> new ArrayList<>()).add(row.getSettlementParticipantId());
        }

        Map<Long, BigDecimal> subtotalBySpId = new HashMap<>();
        for (SettlementParticipant sp : participants) {
            subtotalBySpId.put(sp.getId(), BigDecimal.ZERO);
        }

        for (ReceiptItem item : items) {
            List<Long> selectors = itemIdToSpIds.get(item.getId());
            if (selectors == null || selectors.isEmpty()) {
                selectors = List.of(hostSpId); // 미선택은 호스트 귀속
            }
            BigDecimal totalPrice = safe(item.getTotalPrice());
            BigDecimal each = totalPrice.divide(BigDecimal.valueOf(selectors.size()), 2, RoundingMode.HALF_UP);

            for (Long spId : selectors) {
                subtotalBySpId.put(spId, subtotalBySpId.get(spId).add(each));
            }
        }

        BigDecimal discount = safe(settlement.getDiscountAmount());
        BigDecimal sumSubtotal = subtotalBySpId.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, BigDecimal> discountBySpId = new HashMap<>();
        for (SettlementParticipant sp : participants) {
            BigDecimal sub = subtotalBySpId.get(sp.getId());
            BigDecimal alloc = BigDecimal.ZERO;
            if (discount.signum() > 0 && sumSubtotal.signum() > 0) {
                alloc = sub.multiply(discount).divide(sumSubtotal, 2, RoundingMode.HALF_UP);
            }
            discountBySpId.put(sp.getId(), alloc);
        }

        for (SettlementParticipant sp : participants) {
            BigDecimal sub = subtotalBySpId.get(sp.getId());
            BigDecimal alloc = discountBySpId.get(sp.getId());
            BigDecimal due = sub.subtract(alloc);

            sp.updateAmounts(sub, alloc, due);
        }
    }

    private BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void publishSettlementResultReadyNotification(Long meetingId, Long settlementId) {
        List<Long> recipients = meetingParticipantRepository.findActiveMemberIds(meetingId);
        if (recipients.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                NotificationType.SETTLEMENT_RESULT_READY,
                "정산 결과 도착",
                "정산 결과가 준비됐어요. 금액을 확인해 주세요.",
                "SETTLEMENT",
                meetingId,
                settlementId,
                "/meetings/" + meetingId + "/settlement/result",
                "SETTLEMENT_RESULT_READY:" + meetingId + ":" + settlementId,
                null,
                recipients
        ));
    }
}
