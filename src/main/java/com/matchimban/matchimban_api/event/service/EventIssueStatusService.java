package com.matchimban.matchimban_api.event.service;

import com.matchimban.matchimban_api.event.dto.response.EventIssueStatusResponse;

public interface EventIssueStatusService {

    EventIssueStatusResponse getCurrentStatus(Long memberId, Long eventId);
}
