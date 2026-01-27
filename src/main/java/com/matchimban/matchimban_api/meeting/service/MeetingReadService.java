package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.MeetingDetailResponse;
import com.matchimban.matchimban_api.meeting.dto.MyMeetingsResponse;

public interface MeetingReadService {
    MyMeetingsResponse getMyMeetings(Long memberId, Long cursor, int size);

    MeetingDetailResponse getMeetingDetail(Long memberId, Long meetingId);
}
