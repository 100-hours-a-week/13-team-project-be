package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingResponse;

public interface MeetingParticipationService {
    ParticipateMeetingResponse participateMeeting(Long memberId, ParticipateMeetingRequest request);

    void leaveMeeting(Long memberId, Long meetingId);
}
