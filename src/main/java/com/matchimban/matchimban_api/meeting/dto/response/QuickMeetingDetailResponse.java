package com.matchimban.matchimban_api.meeting.dto.response;

import com.matchimban.matchimban_api.vote.entity.enums.VoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "퀵 모임 상세 조회 응답")
public class QuickMeetingDetailResponse {

    private Long meetingId;
    private String inviteCode;

    private String locationAddress;

    private long participantCount;
    private int targetHeadcount;

    private Instant voteDeadlineAt;

    private Long currentVoteId;
    private VoteStatus voteStatus;

    private Long hostMemberId;
}