package com.matchimban.matchimban_api.settlement.ocr.repository;

import com.matchimban.matchimban_api.settlement.ocr.entity.SettlementOcrJob;
import com.matchimban.matchimban_api.settlement.ocr.enums.OcrJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SettlementOcrJobRepository extends JpaRepository<SettlementOcrJob, Long> {

    Optional<SettlementOcrJob> findFirstBySettlementIdAndStatusInOrderByIdDesc(Long settlementId, java.util.List<OcrJobStatus> statuses);

    @Query(value = """
        select *
        from settlement_ocr_jobs
        where (status = 'PENDING' and next_attempt_at <= now())
           or (status = 'PROCESSING' and lock_until < now())
        order by next_attempt_at asc, created_at asc, id asc
        for update skip locked
        limit 1
        """, nativeQuery = true)
    Optional<SettlementOcrJob> findNextClaimableForUpdate();

    Optional<SettlementOcrJob> findTopBySettlementIdAndStatusOrderByIdDesc(Long settlementId, OcrJobStatus status);

    Optional<SettlementOcrJob> findTopBySettlementIdOrderByIdDesc(Long settlementId);

}