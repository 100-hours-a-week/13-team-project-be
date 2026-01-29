package com.matchimban.matchimban_api.vote.dto.response;

import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoteStatusResponse {
    private VoteStatus voteStatus;
    private long submittedCount;
    private long totalCount;
}