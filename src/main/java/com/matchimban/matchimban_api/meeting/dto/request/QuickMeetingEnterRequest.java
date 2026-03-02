package com.matchimban.matchimban_api.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuickMeetingEnterRequest {

    @NotBlank
    private String inviteCode;

    private String guestUuid;
}