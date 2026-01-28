package com.matchimban.matchimban_api.vote.event;

import com.matchimban.matchimban_api.vote.service.VoteCandidateAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class VoteCandidateGenerationListener {

    private final VoteCandidateAsyncService voteCandidateAsyncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VoteCandidateGenerationRequestedEvent event) {
        voteCandidateAsyncService.generateCandidates(event.meetingId(), event.round1VoteId(), event.round2VoteId());
    }
}
