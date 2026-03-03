package com.matchimban.matchimban_api.settlement.repository;

import com.matchimban.matchimban_api.settlement.entity.SettlementParticipant;
import com.matchimban.matchimban_api.settlement.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SettlementParticipantRepository extends JpaRepository<SettlementParticipant, Long> {

    Optional<SettlementParticipant> findBySettlementIdAndParticipantId(Long settlementId, Long participantId);

    List<SettlementParticipant> findAllBySettlementId(Long settlementId);

    @Query("select count(sp) from SettlementParticipant sp where sp.settlement.id = :settlementId")
    long countAllBySettlementId(Long settlementId);

    @Query("select count(sp) from SettlementParticipant sp where sp.settlement.id = :settlementId and sp.selectionConfirmedAt is not null")
    long countConfirmedBySettlementId(Long settlementId);

    @Query("select count(sp) from SettlementParticipant sp where sp.settlement.id = :settlementId and sp.paymentStatus = :status")
    long countBySettlementIdAndPaymentStatus(Long settlementId, PaymentStatus status);

    interface SettlementResultRow {
        Long getMeetingParticipantId();
        Long getMemberId();
        String getNickname();
        String getProfileImageUrl();
        java.math.BigDecimal getAmountDue();
        PaymentStatus getPaymentStatus();
    }

    @Query("""
        select 
          sp.participant.id as meetingParticipantId,
          sp.participant.member.id as memberId,
          sp.participant.member.nickname as nickname,
          sp.participant.member.profileImageUrl as profileImageUrl,
          sp.amountDue as amountDue,
          sp.paymentStatus as paymentStatus
        from SettlementParticipant sp
        where sp.settlement.id = :settlementId
        order by sp.participant.createdAt asc, sp.participant.id asc
    """)
    List<SettlementResultRow> findResultRows(Long settlementId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SettlementParticipant sp
           set sp.paymentStatus = :status,
               sp.paymentConfirmedAt = :now
         where sp.settlement.id = :settlementId
           and sp.paymentStatus <> :status
    """)
    int bulkSetPaymentDone(Long settlementId, PaymentStatus status, Instant now);
}
