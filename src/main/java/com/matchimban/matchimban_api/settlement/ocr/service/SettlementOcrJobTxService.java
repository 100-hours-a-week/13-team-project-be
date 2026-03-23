package com.matchimban.matchimban_api.settlement.ocr.service;

import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.ReceiptItem;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.ocr.config.AppOcrProperties;
import com.matchimban.matchimban_api.settlement.ocr.dto.RunpodOcrResponse;
import com.matchimban.matchimban_api.settlement.ocr.entity.SettlementOcrJob;
import com.matchimban.matchimban_api.settlement.ocr.enums.OcrJobStatus;
import com.matchimban.matchimban_api.settlement.ocr.repository.SettlementOcrJobRepository;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.ReceiptItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SettlementOcrJobTxService {

    private final SettlementOcrJobRepository jobRepository;
    private final MeetingSettlementRepository settlementRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final AppOcrProperties ocrProps;
    private final com.matchimban.matchimban_api.settlement.service.SettlementProgressSseService settlementProgressSseService;

    @Transactional
    public Long claimNextJob(String instanceId) {
        return jobRepository.findNextClaimableForUpdate()
                .map(job -> {
                    Instant now = Instant.now();

                    if (job.getAttemptCount() >= ocrProps.getMaxAttempts()) {
                        job.markFailed("MAX_ATTEMPTS", "재시도 횟수를 초과했습니다.", now);

                        MeetingSettlement settlement = job.getSettlement();
                        settlement.changeStatus(SettlementStatus.OCR_FAILED);

                        jobRepository.save(job);
                        settlementRepository.save(settlement);
                        settlementProgressSseService.publishAfterCommit(settlement.getMeeting().getId());
                        return null;
                    }

                    Instant lockUntil = now.plus(ocrProps.getLease());
                    job.markProcessing(instanceId, lockUntil, now);
                    jobRepository.save(job);
                    return job.getId();
                })
                .orElse(null);
    }

    @Transactional
    public void requeue(Long jobId, Long settlementId, String code, String message) {
        SettlementOcrJob job = jobRepository.findById(jobId).orElseThrow();
        MeetingSettlement settlement = settlementRepository.findById(settlementId).orElseThrow();

        Instant nextAttemptAt = Instant.now().plus(calculateRetryDelay(job.getAttemptCount()));

        job.requeue(code, message, nextAttemptAt);
        settlement.changeStatus(SettlementStatus.OCR_PROCESSING);

        jobRepository.save(job);
        settlementRepository.save(settlement);
        settlementProgressSseService.publishAfterCommit(settlement.getMeeting().getId());
    }

    @Transactional
    public void fail(Long jobId, Long settlementId, String code, String message) {
        SettlementOcrJob job = jobRepository.findById(jobId).orElseThrow();
        MeetingSettlement settlement = settlementRepository.findById(settlementId).orElseThrow();

        Instant now = Instant.now();

        job.markFailed(code, message, now);
        settlement.changeStatus(SettlementStatus.OCR_FAILED);

        jobRepository.save(job);
        settlementRepository.save(settlement);
        settlementProgressSseService.publishAfterCommit(settlement.getMeeting().getId());
    }

    @Transactional
    public void succeed(Long jobId, Long settlementId, RunpodOcrResponse resp) {
        SettlementOcrJob job = jobRepository.findById(jobId).orElseThrow();
        MeetingSettlement settlement = settlementRepository.findById(settlementId).orElseThrow();

        settlement.applyOcrSummary(
                BigDecimal.valueOf(resp.result().totalAmount()),
                BigDecimal.valueOf(resp.result().discountAmount())
        );

        receiptItemRepository.deleteBySettlementId(settlementId);

        List<ReceiptItem> items = new ArrayList<>(resp.result().items().size());
        for (RunpodOcrResponse.ReceiptItem it : resp.result().items()) {
            items.add(ReceiptItem.builder()
                    .settlement(settlement)
                    .itemName(it.name())
                    .unitPrice(BigDecimal.valueOf(it.unitPrice()))
                    .quantity((int) Math.round(it.quantity()))
                    .totalPrice(BigDecimal.valueOf(it.amount()))
                    .build());
        }

        receiptItemRepository.saveAll(items);

        settlement.changeStatus(SettlementStatus.OCR_SUCCEEDED);
        job.markSucceeded(Instant.now());

        settlementRepository.save(settlement);
        jobRepository.save(job);
        settlementProgressSseService.publishAfterCommit(settlement.getMeeting().getId());
    }

    private Duration calculateRetryDelay(int attemptCount) {
        long initialSeconds = ocrProps.getInitialRetryDelay().getSeconds();
        long maxSeconds = ocrProps.getMaxRetryDelay().getSeconds();

        long multiplier = 1L << Math.max(0, attemptCount - 1);
        long delaySeconds = initialSeconds * multiplier;

        return Duration.ofSeconds(Math.min(delaySeconds, maxSeconds));
    }
}
