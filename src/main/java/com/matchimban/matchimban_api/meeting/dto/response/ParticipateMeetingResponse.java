package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipateMeetingResponse {

    @Schema(description = "참여한 모임 ID")
    private Long meetingId;
}