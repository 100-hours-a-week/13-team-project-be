package com.matchimban.matchimban_api.settlement.ocr.worker;

import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.entity.ReceiptItem;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.ocr.client.RunpodOcrClient;
import com.matchimban.matchimban_api.settlement.ocr.config.AppOcrProperties;
import com.matchimban.matchimban_api.settlement.ocr.dto.RunpodOcrResponse;
import com.matchimban.matchimban_api.settlement.ocr.entity.SettlementOcrJob;
import com.matchimban.matchimban_api.settlement.ocr.enums.OcrJobStatus;
import com.matchimban.matchimban_api.settlement.ocr.repository.SettlementOcrJobRepository;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.repository.ReceiptItemRepository;
import com.matchimban.matchimban_api.settlement.service.S3PresignedGetUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementOcrWorker {

    private final SettlementOcrJobRepository jobRepository;
    private final MeetingSettlementRepository settlementRepository;
    private final ReceiptItemRepository receiptItemRepository;

    private final RunpodOcrClient runpodOcrClient;
    private final S3PresignedGetUrlService s3PresignedGetUrlService;
    private final AppOcrProperties ocrProps;

    private final String instanceId = "ocr-worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${app.ocr.poll-delay:1000ms}")
    public void tick() {
        Long jobId = claimNextJob();
        if (jobId == null) return;
        processJob(jobId);
    }

    @Transactional
    protected Long claimNextJob() {
        return jobRepository.findNextClaimableForUpdate()
                .map(job -> {
                    if (job.getAttemptCount() >= ocrProps.getMaxAttempts()) {
                        job.markFailed("MAX_ATTEMPTS", "재시도 횟수를 초과했습니다.");
                        jobRepository.save(job);
                        return null;
                    }
                    Instant lockUntil = Instant.now().plus(ocrProps.getLease());
                    job.markProcessing(instanceId, lockUntil);
                    jobRepository.save(job);
                    return job.getId();
                })
                .orElse(null);
    }

    protected void processJob(Long jobId) {
        SettlementOcrJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;
        if (job.getStatus() != OcrJobStatus.PROCESSING) return;

        MeetingSettlement settlement = settlementRepository.findById(job.getSettlement().getId()).orElse(null);
        if (settlement == null) {
            finishFailed(jobId, "SETTLEMENT_NOT_FOUND", "정산 정보를 찾을 수 없습니다.");
            return;
        }

        try {
            String objectKey = settlement.getReceiptImageUrl();
            String imageUrl = s3PresignedGetUrlService.presignGet(objectKey, ocrProps.getTimeout().plusSeconds(30));

            RunpodOcrResponse resp = runpodOcrClient.requestReceiptOcr(imageUrl, job.getRequestId());

            if (resp == null) {
                finishFailed(jobId, "NULL_RESPONSE", "OCR 서버 응답이 없습니다.");
                markSettlementFailed(settlement.getId());
                return;
            }

            if (resp.error() != null) {
                finishFailed(jobId, resp.error().code(), resp.error().message());
                markSettlementFailed(settlement.getId());
                return;
            }

            if (resp.result() == null) {
                finishFailed(jobId, "EMPTY_RESULT", "OCR 결과가 비어 있습니다.");
                markSettlementFailed(settlement.getId());
                return;
            }

            saveOcrResult(settlement.getId(), resp);
            finishSucceeded(jobId);
            markSettlementSucceeded(settlement.getId());

        } catch (Exception e) {
            finishFailed(jobId, "NETWORK_OR_PARSE", e.getMessage() == null ? "OCR 호출 실패" : e.getMessage());
            markSettlementFailed(settlement.getId());
        }
    }

    @Transactional
    protected void saveOcrResult(Long settlementId, RunpodOcrResponse resp) {
        MeetingSettlement settlement = settlementRepository.findById(settlementId).orElseThrow();

        settlement.applyOcrSummary(
                java.math.BigDecimal.valueOf(resp.result().totalAmount()),
                java.math.BigDecimal.valueOf(resp.result().discountAmount())
        );

        receiptItemRepository.deleteBySettlementId(settlementId);

        for (RunpodOcrResponse.ReceiptItem it : resp.result().items()) {
            ReceiptItem item = ReceiptItem.builder()
                    .settlement(settlement)
                    .itemName(it.name())
                    .unitPrice(BigDecimal.valueOf(it.unitPrice()))
                    .quantity((int) Math.round(it.quantity()))
                    .totalPrice(BigDecimal.valueOf(it.amount()))
                    .build();
            receiptItemRepository.save(item);
        }

        settlement.changeStatus(SettlementStatus.OCR_SUCCEEDED);
        settlementRepository.save(settlement);
    }

    @Transactional
    protected void markSettlementSucceeded(Long settlementId) {
        settlementRepository.findById(settlementId).ifPresent(s -> {
            s.changeStatus(SettlementStatus.OCR_SUCCEEDED);
            settlementRepository.save(s);
        });
    }

    @Transactional
    protected void markSettlementFailed(Long settlementId) {
        settlementRepository.findById(settlementId).ifPresent(s -> {
            s.changeStatus(SettlementStatus.OCR_FAILED);
            settlementRepository.save(s);
        });
    }

    @Transactional
    protected void finishSucceeded(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.markSucceeded();
            jobRepository.save(job);
        });
    }

    @Transactional
    protected void finishFailed(Long jobId, String code, String message) {
        jobRepository.findById(jobId).ifPresent(j -> {
            j.markFailed(code, message);
            jobRepository.save(j);
        });
    }
}
