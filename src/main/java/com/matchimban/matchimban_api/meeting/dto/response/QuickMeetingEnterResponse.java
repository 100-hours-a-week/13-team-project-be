package com.matchimban.matchimban_api.meeting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class QuickMeetingEnterResponse {

    private Long meetingId;
    private String guestUuid;
    private Instant voteDeadlineAt;
}