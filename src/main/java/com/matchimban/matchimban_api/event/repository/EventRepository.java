package com.matchimban.matchimban_api.event.repository;

import com.matchimban.matchimban_api.event.entity.Event;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {

    Optional<Event> findByIdAndIsActiveTrueAndIsDeletedFalse(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.id = :eventId
              AND e.isActive = true
              AND e.isDeleted = false
            """)
    Optional<Event> findByIdForIssue(Long eventId);

    @Query("""
            SELECT e.id
            FROM Event e
            WHERE e.isActive = true
              AND e.isDeleted = false
              AND e.startAt <= :now
              AND e.endAt > :now
            ORDER BY e.id ASC
            """)
    List<Long> findIssueTargetEventIds(Instant now);
}
