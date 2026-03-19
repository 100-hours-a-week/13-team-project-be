package com.matchimban.matchimban_api.event.service;

import com.matchimban.matchimban_api.event.dto.response.EventIssueRequestResponse;

public interface EventIssueRequestService {

    EventIssueRequestResponse submit(Long memberId, Long eventId);
}
