package com.matchimban.matchimban_api.meeting.dto;

import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "모임 상세 조회 응답")
public class MeetingDetailResponse {

    @Schema(description = "모임 ID")
    private Long meetingId;

    @Schema(description = "모임 이름")
    private String title;

    @Schema(description = "모임 일시")
    private LocalDateTime scheduledAt;

    @Schema(description = "투표 마감 일시")
    private LocalDateTime voteDeadlineAt;

    @Schema(description = "모임 주소")
    private String locationAddress;

    @Schema(description = "모임 장소 위도")
    private BigDecimal locationLat;

    @Schema(description = "모임 장소 경도")
    private BigDecimal locationLng;

    @Schema(description = "목표 인원")
    private int targetHeadcount;

    @Schema(description = "탐색 범위(m)")
    private int searchRadiusM;

    @Schema(description = "스와이프 수")
    private int swipeCount;

    @Schema(description = "고깃집 제외 여부")
    private boolean exceptMeat;

    @Schema(description = "술집 제외 여부")
    private boolean exceptBar;

    @Schema(description = "퀵 모임 여부")
    private boolean quickMeeting;

    @Schema(description = "초대 코드")
    private String inviteCode;

    @Schema(description = "호스트 memberId")
    private Long hostMemberId;

    @Schema(description = "현재 참여자 수(ACTIVE 기준)")
    private long participantCount;

    @Schema(description = "참여자 목록")
    private List<MeetingParticipantSummary> participants;

    @Schema(description = "현재(최신) 투표 ID")
    private Long currentVoteId;

    @Schema(description = "현재(최신) 투표 상태")
    private VoteStatus voteStatus;

    @Schema(description = "최종 선택 완료 여부")
    private boolean finalSelected;

    @Schema(description = "모임 진행 상태")
    private MeetingStatus meetingStatus;
}