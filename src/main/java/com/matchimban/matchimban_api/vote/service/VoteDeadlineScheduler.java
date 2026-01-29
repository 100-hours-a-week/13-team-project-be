package com.matchimban.matchimban_api.vote.service;

import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoteDeadlineScheduler {

    private final VoteRepository voteRepository;
    private final VoteCountService voteCountService;

    @Scheduled(fixedDelay = 30_000)
    public void triggerCountForExpiredVotes() {
        List<Long> voteIds = voteRepository.findOpenVoteIdsPastDeadline(LocalDateTime.now());
        for (Long voteId : voteIds) {
            boolean started = voteCountService.tryStartCounting(voteId);
            if (started) {
                voteCountService.countAsync(voteId);
            }
        }
    }
}
