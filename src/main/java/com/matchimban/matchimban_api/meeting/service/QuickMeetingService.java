package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.request.QuickMeetingEnterRequest;
import com.matchimban.matchimban_api.meeting.dto.response.QuickMeetingEnterResponse;
import org.springframework.http.ResponseCookie;

public interface QuickMeetingService {

    record EnterResult(QuickMeetingEnterResponse body, ResponseCookie guestAccessCookie) {}

    EnterResult enter(Object principal, QuickMeetingEnterRequest request);
}