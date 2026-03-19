package com.matchimban.matchimban_api.event.repository;

import com.matchimban.matchimban_api.event.entity.EventCoupon;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EventCouponRepository extends JpaRepository<EventCoupon, Long> {

    List<EventCoupon> findAllByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(Long memberId);

    @Query("""
            SELECT ec
            FROM EventCoupon ec
            JOIN FETCH ec.eventParticipant ep
            JOIN FETCH ep.event e
            WHERE ec.member.id = :memberId
              AND ec.isDeleted = false
              AND (
                    ec.createdAt < :cursorCreatedAt
                    OR (ec.createdAt = :cursorCreatedAt AND ec.id < :cursorId)
              )
            ORDER BY ec.createdAt DESC, ec.id DESC
            """)
    List<EventCoupon> findMyCouponPage(
            @Param("memberId") Long memberId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    boolean existsByMemberIdAndEventParticipantEventIdAndIsDeletedFalse(Long memberId, Long eventId);

    @Query("""
            SELECT ec.member.id
            FROM EventCoupon ec
            WHERE ec.eventParticipant.event.id = :eventId
              AND ec.isDeleted = false
            """)
    List<Long> findIssuedMemberIdsByEventId(@Param("eventId") Long eventId);
}
