package com.matchimban.matchimban_api.vote.repository;

import com.matchimban.matchimban_api.vote.entity.VoteChoice;
import com.matchimban.matchimban_api.vote.entity.VoteSubmission;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteSubmissionRepository extends JpaRepository<VoteSubmission, Long> {

    boolean existsByVoteIdAndParticipantId(Long voteId, Long participantId);

    long countByVoteId(Long voteId);

    List<VoteSubmission> findByVoteIdAndParticipantId(Long voteId, Long participantId);

    @Query("""
        select count(distinct vs.participant.id)
        from VoteSubmission vs
        where vs.vote.id = :voteId
    """)
    long countDistinctParticipantsByVoteId(@Param("voteId") Long voteId);

    interface CandidateChoiceCountRow {
        Long getCandidateId();
        VoteChoice getChoice();
        long getCnt();
    }

    @Query("""
        select vs.candidateRestaurant.id as candidateId,
               vs.choice as choice,
               count(vs.id) as cnt
        from VoteSubmission vs
        where vs.vote.id = :voteId
        group by vs.candidateRestaurant.id, vs.choice
    """)
    List<CandidateChoiceCountRow> countByCandidateAndChoice(@Param("voteId") Long voteId);

}
