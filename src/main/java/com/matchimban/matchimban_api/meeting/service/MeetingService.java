package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.request.CreateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.response.CreateMeetingResponse;
import com.matchimban.matchimban_api.meeting.dto.request.UpdateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.response.UpdateMeetingResponse;

public interface MeetingService {
    CreateMeetingResponse createMeeting(Long memberId, CreateMeetingRequest req);

    UpdateMeetingResponse updateMeeting(Long memberId, Long meetingId, UpdateMeetingRequest req);

    void deleteMeeting(Long memberId, Long meetingId);
}
