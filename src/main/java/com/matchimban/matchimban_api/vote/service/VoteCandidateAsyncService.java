package com.matchimban.matchimban_api.vote.service;

public interface VoteCandidateAsyncService {

    void generateCandidates(Long meetingId, Long round1VoteId, Long round2VoteId);

    void markVotesFailedInNewTx(Long round1VoteId, Long round2VoteId);
}
