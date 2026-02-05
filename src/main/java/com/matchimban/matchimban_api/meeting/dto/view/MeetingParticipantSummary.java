package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "모임 참여자 표시용 정보")
public class MeetingParticipantSummary {

    @Schema(description = "참여자 memberId", example = "10")
    private Long memberId;

    @Schema(description = "참여자 닉네임", example = "hazel")
    private String nickname;

    @Schema(description = "프로필 이미지 URL(없으면 null)", example = "https://.../profile.jpg", nullable = true)
    private String profileImageUrl;
}