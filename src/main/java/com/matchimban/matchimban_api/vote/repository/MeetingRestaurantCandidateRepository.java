package com.matchimban.matchimban_api.vote.repository;

import com.matchimban.matchimban_api.vote.dto.response.VoteCandidatesResponse;
import com.matchimban.matchimban_api.vote.dto.response.VoteResultsResponse;
import com.matchimban.matchimban_api.vote.entity.MeetingRestaurantCandidate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRestaurantCandidateRepository extends JpaRepository<MeetingRestaurantCandidate, Long> {

    void deleteByVoteId(Long voteId);

    boolean existsByVoteId(Long voteId);

    @Query("""
        select new com.matchimban.matchimban_api.vote.dto.response.VoteCandidatesResponse$Candidate(
            c.id,
            r.name,
            r.imageUrl1,
            r.imageUrl2,
            r.imageUrl3,
            c.distanceM,
            c.rating,
            fc.categoryName,
            r.roadAddress,
            r.jibunAddress
        )
        from MeetingRestaurantCandidate c
        join c.restaurant r
        join r.foodCategory fc
        where c.vote.id = :voteId
        order by c.baseRank asc, c.id asc
    """)
    List<VoteCandidatesResponse.Candidate> findCandidateDtosByVoteId(@Param("voteId") Long voteId);

    boolean existsByVoteIdAndId(Long voteId, Long id);

    @org.springframework.data.jpa.repository.Query("""
        select c.id
        from MeetingRestaurantCandidate c
        where c.vote.id = :voteId
          and c.id in :candidateIds
    """)
    List<Long> findIdsByVoteIdAndIdIn(
            @org.springframework.data.repository.query.Param("voteId") Long voteId,
            @org.springframework.data.repository.query.Param("candidateIds") List<Long> candidateIds
    );

    @Query("""
        select c.id
        from MeetingRestaurantCandidate c
        where c.vote.id = :voteId
    """)
    List<Long> findCandidateIdsByVoteId(@Param("voteId") Long voteId);

    @Query("""
        select new com.matchimban.matchimban_api.vote.dto.response.VoteResultsResponse$Item(
            c.id,
            c.resultRank,
            r.name,
            r.imageUrl1,
            fc.categoryName,
            c.rating,
            c.likeCount,
            c.distanceM,
            r.roadAddress,
            r.jibunAddress
        )
        from MeetingRestaurantCandidate c
        join c.restaurant r
        join r.foodCategory fc
        where c.vote.id = :voteId
          and c.resultRank between 1 and 3
        order by c.resultRank asc
    """)
    List<VoteResultsResponse.Item> findTop3ResultItems(@Param("voteId") Long voteId);

    @Query("""
        select c
        from MeetingRestaurantCandidate c
        join fetch c.vote v
        join fetch v.meeting m
        where c.id = :candidateId
    """)
    Optional<MeetingRestaurantCandidate> findByIdWithVoteAndMeeting(@Param("candidateId") Long candidateId);

}