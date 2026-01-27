package com.matchimban.matchimban_api.meeting.repository.projection;

import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyMeetingRow {
    private Long meetingParticipantId;
    private Long meetingId;
    private String title;
    private LocalDateTime scheduledAt;
    private Long participantCount;
    private Integer targetHeadcount;
    private VoteStatus voteStatus;
}