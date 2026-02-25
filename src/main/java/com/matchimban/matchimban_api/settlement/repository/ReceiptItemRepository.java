package com.matchimban.matchimban_api.settlement.repository;

import com.matchimban.matchimban_api.settlement.entity.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {
    List<ReceiptItem> findAllBySettlementIdOrderByIdAsc(Long settlementId);

    void deleteBySettlementId(Long settlementId);

    @Query("""
        select ri
        from ReceiptItem ri
        where ri.settlement.id = :settlementId
          and ri.id in :itemIds
    """)
    List<ReceiptItem> findAllBySettlementIdAndIdIn(Long settlementId, List<Long> itemIds);

}