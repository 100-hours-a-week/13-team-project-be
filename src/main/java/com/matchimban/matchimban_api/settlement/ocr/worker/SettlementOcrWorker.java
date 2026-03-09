package com.matchimban.matchimban_api.settlement.ocr.worker;

import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.ocr.client.OcrClientException;
import com.matchimban.matchimban_api.settlement.ocr.client.RunpodOcrClient;
import com.matchimban.matchimban_api.settlement.ocr.config.AppOcrProperties;
import com.matchimban.matchimban_api.settlement.ocr.dto.RunpodOcrResponse;
import com.matchimban.matchimban_api.settlement.ocr.entity.SettlementOcrJob;
import com.matchimban.matchimban_api.settlement.ocr.enums.OcrJobStatus;
import com.matchimban.matchimban_api.settlement.ocr.repository.SettlementOcrJobRepository;
import com.matchimban.matchimban_api.settlement.ocr.service.SettlementOcrJobTxService;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.settlement.service.S3PresignedGetUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementOcrWorker {

    private final SettlementOcrJobRepository jobRepository;
    private final MeetingSettlementRepository settlementRepository;
    private final SettlementOcrJobTxService settlementOcrJobTxService;

    private final RunpodOcrClient runpodOcrClient;
    private final S3PresignedGetUrlService s3PresignedGetUrlService;
    private final AppOcrProperties ocrProps;

    private final String instanceId = "ocr-worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${app.ocr.poll-delay:1000ms}")
    public void tick() {
        Long jobId = settlementOcrJobTxService.claimNextJob(instanceId);
        if (jobId == null) return;
        processJob(jobId);
    }

    protected void processJob(Long jobId) {
        SettlementOcrJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;
        if (job.getStatus() != OcrJobStatus.PROCESSING) return;

        MeetingSettlement settlement = settlementRepository.findById(job.getSettlement().getId()).orElse(null);
        if (settlement == null) {
            settlementOcrJobTxService.fail(jobId, job.getSettlement().getId(), "SETTLEMENT_NOT_FOUND", "정산 정보를 찾을 수 없습니다.");
            return;
        }

        try {
            runpodOcrClient.assertHealthy();

            String objectKey = settlement.getReceiptImageUrl();
            String imageUrl = s3PresignedGetUrlService.presignGet(objectKey, ocrProps.getTimeout().plusSeconds(30));

            RunpodOcrResponse resp = runpodOcrClient.requestReceiptOcr(imageUrl, job.getRequestId());

            if (resp.error() != null) {
                settlementOcrJobTxService.fail(jobId, settlement.getId(), resp.error().code(), resp.error().message());
                return;
            }

            if (resp.result() == null) {
                settlementOcrJobTxService.fail(jobId, settlement.getId(), "EMPTY_RESULT", "OCR 결과가 비어 있습니다.");
                return;
            }

            settlementOcrJobTxService.succeed(jobId, settlement.getId(), resp);

        } catch (OcrClientException e) {
            if (e.isRetryable() && job.getAttemptCount() < ocrProps.getMaxAttempts()) {
                settlementOcrJobTxService.requeue(jobId, settlement.getId(), e.getCode(), e.getMessage());
                return;
            }

            settlementOcrJobTxService.fail(jobId, settlement.getId(), e.getCode(), e.getMessage());

        } catch (Exception e) {
            String message = e.getMessage() == null ? "OCR 호출 실패" : e.getMessage();

            if (job.getAttemptCount() < ocrProps.getMaxAttempts()) {
                settlementOcrJobTxService.requeue(jobId, settlement.getId(), "OCR_UNEXPECTED", message);
                return;
            }

            settlementOcrJobTxService.fail(jobId, settlement.getId(), "OCR_UNEXPECTED", message);
        }
    }

}
