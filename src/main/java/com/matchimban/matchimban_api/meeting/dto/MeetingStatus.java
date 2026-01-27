package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 진행 상태")
public enum MeetingStatus {
    READY,
    VOTING,
    DONE
}