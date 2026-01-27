package com.matchimban.matchimban_api.vote.repository;

import com.matchimban.matchimban_api.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    Optional<Vote> findTopByMeetingIdOrderByRoundDesc(Long meetingId);

    boolean existsByMeetingId(Long meetingId);
}
