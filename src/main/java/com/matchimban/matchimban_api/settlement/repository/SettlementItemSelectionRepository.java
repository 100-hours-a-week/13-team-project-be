package com.matchimban.matchimban_api.settlement.repository;

import com.matchimban.matchimban_api.settlement.entity.SettlementItemSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SettlementItemSelectionRepository extends JpaRepository<SettlementItemSelection, Long> {

    @Query("""
        select sis.item.id
        from SettlementItemSelection sis
        where sis.settlementParticipant.id = :settlementParticipantId
        order by sis.item.id asc
    """)
    List<Long> findItemIdsBySettlementParticipantId(Long settlementParticipantId);

    interface SelectionRow {
        Long getSettlementParticipantId();
        Long getItemId();
        java.math.BigDecimal getItemTotalPrice();
    }

    @Query("""
        select 
          sis.settlementParticipant.id as settlementParticipantId,
          sis.item.id as itemId,
          sis.item.totalPrice as itemTotalPrice
        from SettlementItemSelection sis
        where sis.settlementParticipant.settlement.id = :settlementId
    """)
    List<SelectionRow> findSelectionRowsBySettlementId(Long settlementId);
}