package com.matchimban.matchimban_api.vote.repository;

import com.matchimban.matchimban_api.vote.entity.Vote;
import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findTopByMeetingIdOrderByRoundDesc(Long meetingId);

    boolean existsByMeetingId(Long meetingId);

    Optional<Vote> findByMeetingIdAndRound(Long meetingId, short round);

    boolean existsByMeetingIdAndRound(Long meetingId, short round);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Vote v
           set v.status = :toStatus
         where v.id = :voteId
           and v.status = :fromStatus
    """)
    int updateStatusIfMatch(
            @Param("voteId") Long voteId,
            @Param("fromStatus") VoteStatus fromStatus,
            @Param("toStatus") VoteStatus toStatus
    );

    @Query("""
        select v.id
        from Vote v
        join v.meeting m
        where v.status = com.matchimban.matchimban_api.vote.entity.VoteStatus.OPEN
          and m.voteDeadlineAt <= :now
    """)
    List<Long> findOpenVoteIdsPastDeadline(@Param("now") LocalDateTime now);

}
