package com.matchimban.matchimban_api.settlement.service;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.settlement.config.AppS3Properties;
import com.matchimban.matchimban_api.settlement.dto.response.SettlementItemsResponse;
import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.ReceiptItem;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.error.SettlementErrorCode;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.ReceiptItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementItemsService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final ReceiptItemRepository receiptItemRepository;

    private final S3PresignedGetUrlService presignedGetUrlService;
    private final AppS3Properties s3Props;

    public SettlementItemsResponse getItems(Long meetingId, Long memberId) {
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

        if (settlement.getSettlementStatus() != SettlementStatus.OCR_SUCCEEDED) {
            throw new ApiException(SettlementErrorCode.OCR_RESULT_NOT_READY);
        }

        List<ReceiptItem> items = receiptItemRepository.findAllBySettlementIdOrderByIdAsc(settlement.getId());

        String receiptImageUrl = buildReceiptImageUrl(settlement.getReceiptImageUrl());

        List<SettlementItemsResponse.Item> mapped = items.stream()
                .map(it -> new SettlementItemsResponse.Item(
                        it.getId(),
                        it.getItemName(),
                        it.getUnitPrice(),
                        it.getQuantity(),
                        it.getTotalPrice()
                ))
                .toList();

        return new SettlementItemsResponse(
                settlement.getId(),
                receiptImageUrl,
                settlement.getTotalAmount(),
                settlement.getDiscountAmount(),
                mapped
        );
    }

    private String buildReceiptImageUrl(String receiptImageUrlOrObjectKey) {
        if (receiptImageUrlOrObjectKey == null || receiptImageUrlOrObjectKey.isBlank()) {
            return null;
        }
        if (receiptImageUrlOrObjectKey.startsWith("http://") || receiptImageUrlOrObjectKey.startsWith("https://")) {
            return receiptImageUrlOrObjectKey;
        }

        Duration ttl = s3Props.getPresignUploadTtl();
        return presignedGetUrlService.presignGet(receiptImageUrlOrObjectKey, ttl);
    }
}