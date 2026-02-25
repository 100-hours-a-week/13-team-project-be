package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.request.ParticipateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.response.ParticipateMeetingResponse;

public interface MeetingParticipationService {
    ParticipateMeetingResponse participateMeeting(Long memberId, ParticipateMeetingRequest request);

    void leaveMeeting(Long memberId, Long meetingId);
}
