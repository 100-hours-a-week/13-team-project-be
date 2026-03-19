package com.matchimban.matchimban_api.event.repository;

import com.matchimban.matchimban_api.event.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    boolean existsByEventIdAndMemberId(Long eventId, Long memberId);

    Optional<EventParticipant> findByEventIdAndMemberId(Long eventId, Long memberId);
}