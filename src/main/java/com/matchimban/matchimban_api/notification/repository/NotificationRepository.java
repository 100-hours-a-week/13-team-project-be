package com.matchimban.matchimban_api.notification.repository;

import com.matchimban.matchimban_api.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        select n
        from Notification n
        where n.member.id = :memberId
          and n.isDeleted = false
          and (n.createdAt < :cursorCreatedAt
            or (n.createdAt = :cursorCreatedAt and n.id < :cursorId))
        order by n.createdAt desc, n.id desc
    """)
    List<Notification> findFeedPage(
            @Param("memberId") Long memberId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    Optional<Notification> findByIdAndMemberIdAndIsDeletedFalse(Long id, Long memberId);

    long countByMemberIdAndIsDeletedFalseAndReadAtIsNull(Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
           set n.readAt = :now
         where n.member.id = :memberId
           and n.isDeleted = false
           and n.readAt is null
    """)
    int markAllRead(@Param("memberId") Long memberId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Notification n
         where (n.isDeleted = true and n.deletedAt < :deletedCutoff)
            or (n.createdAt < :createdCutoff)
    """)
    int cleanupExpired(@Param("deletedCutoff") Instant deletedCutoff, @Param("createdCutoff") Instant createdCutoff);
}
