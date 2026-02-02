package com.matchimban.matchimban_api.vote.service;

public interface VoteCountService {

    boolean tryStartCounting(Long voteId);

    void countSync(Long voteId);
}
