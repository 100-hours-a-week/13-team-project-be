package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.meeting.dto.response.InviteCodeResponse;
import com.matchimban.matchimban_api.meeting.dto.response.MeetingDetailResponse;
import com.matchimban.matchimban_api.meeting.dto.response.MeetingDetailStateResponse;
import com.matchimban.matchimban_api.meeting.dto.response.MyMeetingsResponse;

public interface MeetingReadService {
    MyMeetingsResponse getMyMeetings(Long memberId, Long cursor, int size);

    MeetingDetailResponse getMeetingDetail(Long memberId, Long meetingId);

    MeetingDetailStateResponse getMeetingDetailState(Long memberId, Long meetingId);

    InviteCodeResponse getInviteCode(Long memberId, Long meetingId);
}
