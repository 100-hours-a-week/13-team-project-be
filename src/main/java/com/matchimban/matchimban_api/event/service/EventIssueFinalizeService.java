package com.matchimban.matchimban_api.event.service;

import com.matchimban.matchimban_api.event.entity.EventIssueFailureReason;
import java.time.Instant;

public interface EventIssueFinalizeService {

    FinalizeResult finalizeIssue(Long eventId, Long memberId);

    record FinalizeResult(
            boolean success,
            EventIssueFailureReason failureReason,
            Long couponId,
            Instant issuedAt,
            Instant expiredAt
    ) {
    }
}
