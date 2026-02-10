package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateMeetingResponse {

    @Schema(description = "생성된 모임 ID")
    private Long meetingId;

    @Schema(description = "초대 코드")
    private String inviteCode;
}