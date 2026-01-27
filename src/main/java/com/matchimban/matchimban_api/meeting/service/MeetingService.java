package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.CreateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingResponse;
import com.matchimban.matchimban_api.meeting.dto.UpdateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.UpdateMeetingResponse;

public interface MeetingService {
    CreateMeetingResponse createMeeting(Long memberId, CreateMeetingRequest req);

    UpdateMeetingResponse updateMeeting(Long memberId, Long meetingId, UpdateMeetingRequest req);

    void deleteMeeting(Long memberId, Long meetingId);
}
