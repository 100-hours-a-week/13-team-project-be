package com.matchimban.matchimban_api.settlement.repository;

import com.matchimban.matchimban_api.settlement.entity.MeetingSettlement;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MeetingSettlementRepository extends JpaRepository<MeetingSettlement, Long> {
    Optional<MeetingSettlement> findByMeetingId(Long meetingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update MeetingSettlement ms
           set ms.settlementStatus = :next
         where ms.id = :settlementId
           and ms.settlementStatus = :current
    """)
    int updateStatusIfCurrent(Long settlementId, SettlementStatus current, SettlementStatus next);

}