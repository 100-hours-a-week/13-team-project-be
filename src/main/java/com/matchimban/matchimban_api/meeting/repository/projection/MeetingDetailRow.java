package com.matchimban.matchimban_api.meeting.repository.projection;

import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MeetingDetailRow {
    private Long meetingId;
    private String title;
    private LocalDateTime scheduledAt;
    private LocalDateTime voteDeadlineAt;

    private String locationAddress;
    private BigDecimal locationLat;
    private BigDecimal locationLng;

    private int targetHeadcount;
    private int searchRadiusM;
    private int swipeCount;
    private boolean exceptMeat;
    private boolean exceptBar;
    private boolean quickMeeting;

    private String inviteCode;
    private Long hostMemberId;

    private long participantCount;
    private Long currentVoteId;
    private VoteStatus voteState;

    private boolean finalSelected;
}