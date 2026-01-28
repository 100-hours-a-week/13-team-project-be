package com.matchimban.matchimban_api.vote.event;

public record VoteCandidateGenerationRequestedEvent(
        Long meetingId,
        Long round1VoteId,
        Long round2VoteId
) {}
