package com.matchimban.matchimban_api.meeting.dto;

import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "모임 상세 상태 조회 응답(폴링용)")
public class MeetingDetailStateResponse {

    @Schema(description = "현재 참여자 수(ACTIVE 기준)")
    private long participantCount;

    @Schema(description = "현재(진입점) 투표 ID")
    private Long currentVoteId;

    @Schema(description = "현재(진입점) 투표 상태")
    private VoteStatus voteStatus;

    @Schema(description = "현재 투표에 로그인한 사용자가 투표 제출했는지 여부")
    private boolean hasVotedCurrent;

    @Schema(description = "최종 선택 완료 여부")
    private boolean finalSelected;

    @Schema(description = "모임 진행 상태")
    private MeetingStatus meetingStatus;

    @Schema(description = "참여자 목록(READY 상태에서만 포함, 그 외 null)")
    private List<MeetingParticipantSummary> participants;
}
