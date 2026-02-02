package com.matchimban.matchimban_api.vote.service;

import com.matchimban.matchimban_api.vote.entity.Vote;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteFailureService {

    private final VoteRepository voteRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markVotesFailed(Long voteId1, Long voteId2) {
        voteRepository.findById(voteId1).ifPresent(Vote::markFailed);
        voteRepository.findById(voteId2).ifPresent(Vote::markFailed);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markVoteFailed(Long voteId) {
        voteRepository.findById(voteId).ifPresent(Vote::markFailed);
    }
}
