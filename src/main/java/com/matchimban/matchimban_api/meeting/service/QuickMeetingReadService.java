package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.response.QuickMeetingDetailResponse;

public interface QuickMeetingReadService {
    QuickMeetingDetailResponse getQuickMeetingDetailByInviteCode(Object principal, String inviteCode);
}