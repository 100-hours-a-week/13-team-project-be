package com.matchimban.matchimban_api.vote.service;

import com.matchimban.matchimban_api.vote.dto.request.FinalSelectionRequest;
import com.matchimban.matchimban_api.vote.dto.request.VoteSubmitRequest;
import com.matchimban.matchimban_api.vote.dto.response.*;

public interface VoteService {

    CreateVoteResponse createVote(Long meetingId, Long memberId);

    VoteCandidatesResponse getCandidates(Long meetingId, Long voteId, Long memberId);

    void submitVote(Long meetingId, Long voteId, Long memberId, VoteSubmitRequest request);

    VoteStatusResponse getVoteStatus(Long meetingId, Long voteId, Long memberId);

    VoteResultsResponse getResults(Long meetingId, Long voteId, Long memberId);

    void startRevote(Long meetingId, Long voteId, Long memberId);

    void finalizeSelection(Long meetingId, Long voteId, Long memberId, FinalSelectionRequest request);

    FinalSelectionResponse getFinalSelection(Long meetingId, Long memberId);
}
