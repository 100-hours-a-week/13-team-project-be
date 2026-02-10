package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "모임 수정 응답")
public class UpdateMeetingResponse {

    @Schema(description = "수정된 모임 ID")
    private Long meetingId;
}
