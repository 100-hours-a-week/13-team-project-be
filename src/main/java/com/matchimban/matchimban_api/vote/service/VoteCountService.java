package com.matchimban.matchimban_api.vote.service;

import com.matchimban.matchimban_api.vote.entity.MeetingRestaurantCandidate;
import com.matchimban.matchimban_api.vote.entity.Vote;
import com.matchimban.matchimban_api.vote.entity.VoteChoice;
import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import com.matchimban.matchimban_api.vote.repository.MeetingRestaurantCandidateRepository;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import com.matchimban.matchimban_api.vote.repository.VoteSubmissionRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteCountService {

    private static final Logger LOG = LoggerFactory.getLogger(VoteCountService.class);

    private final VoteRepository voteRepository;
    private final VoteSubmissionRepository voteSubmissionRepository;
    private final MeetingRestaurantCandidateRepository candidateRepository;

    @Transactional
    public boolean tryStartCounting(Long voteId) {
        int updated = voteRepository.updateStatusIfMatch(voteId, VoteStatus.OPEN, VoteStatus.COUNTING);
        return updated == 1;
    }

    @Async
    @Transactional
    public void countAsync(Long voteId) {
        try {
            Vote vote = voteRepository.findById(voteId).orElseThrow();

            if (vote.getStatus() != VoteStatus.COUNTING) {
                return;
            }

            List<Long> candidateIds = candidateRepository.findCandidateIdsByVoteId(voteId);
            if (candidateIds.isEmpty()) {
                vote.markFailed();
                return;
            }

            List<MeetingRestaurantCandidate> candidates = candidateRepository.findAllById(candidateIds);
            for (MeetingRestaurantCandidate c : candidates) {
                c.applyCounts(0, 0, 0);
            }

            var rows = voteSubmissionRepository.countByCandidateAndChoice(voteId);

            Map<Long, Integer> likeMap = new HashMap<>();
            Map<Long, Integer> dislikeMap = new HashMap<>();
            Map<Long, Integer> neutralMap = new HashMap<>();

            for (var row : rows) {
                long cnt = row.getCnt();
                Long cid = row.getCandidateId();

                if (row.getChoice() == VoteChoice.LIKE) {
                    likeMap.put(cid, (int) cnt);
                } else if (row.getChoice() == VoteChoice.DISLIKE) {
                    dislikeMap.put(cid, (int) cnt);
                } else if (row.getChoice() == VoteChoice.NEUTRAL) {
                    neutralMap.put(cid, (int) cnt);
                }
            }

            for (MeetingRestaurantCandidate c : candidates) {
                int like = likeMap.getOrDefault(c.getId(), 0);
                int dislike = dislikeMap.getOrDefault(c.getId(), 0);
                int neutral = neutralMap.getOrDefault(c.getId(), 0);
                c.applyCounts(like, dislike, neutral);
            }

            candidates.sort(
                    Comparator
                            .comparingInt((MeetingRestaurantCandidate c) ->
                                    (c.getLikeCount() == null ? 0 : c.getLikeCount())
                                            - (c.getDislikeCount() == null ? 0 : c.getDislikeCount())
                            ).reversed()
                            .thenComparing(
                                    (MeetingRestaurantCandidate c) ->
                                            c.getAiScore() == null ? java.math.BigDecimal.ZERO : c.getAiScore(),
                                    Comparator.reverseOrder()
                            )
                            .thenComparingInt(c -> c.getBaseRank() == null ? Integer.MAX_VALUE : c.getBaseRank())
                            .thenComparingLong(c -> c.getId() == null ? Long.MAX_VALUE : c.getId())
            );

            int rank = 1;
            for (MeetingRestaurantCandidate c : candidates) {
                c.applyResultRank(rank++);
            }

            vote.markCounted(LocalDateTime.now());

            LOG.info("Vote counted. voteId={}, candidates={}", voteId, candidates.size());

        } catch (Exception e) {
            LOG.error("Vote count failed. voteId={}", voteId, e);
            voteRepository.findById(voteId).ifPresent(Vote::markFailed);
        }
    }
}
