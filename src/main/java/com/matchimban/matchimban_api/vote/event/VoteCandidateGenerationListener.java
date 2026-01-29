package com.matchimban.matchimban_api.vote.event;

import com.matchimban.matchimban_api.vote.service.VoteCandidateAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteCandidateGenerationListener {

    private final VoteCandidateAsyncService voteCandidateAsyncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VoteCandidateGenerationRequestedEvent event) {
        log.info("[VoteGenListener] start meetingId={}, v1={}, v2={}",
                event.meetingId(), event.round1VoteId(), event.round2VoteId());
        try {
            voteCandidateAsyncService.generateCandidates(event.meetingId(), event.round1VoteId(), event.round2VoteId());
            log.info("[VoteGenListener] success meetingId={}", event.meetingId());
        } catch (Exception e) {
            log.error("[VoteGenListener] failed meetingId={}", event.meetingId(), e);
            throw e;
        }
    }
}
