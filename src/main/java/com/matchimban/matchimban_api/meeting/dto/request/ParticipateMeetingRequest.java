package com.matchimban.matchimban_api.meeting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParticipateMeetingRequest {

    @Schema(description = "초대 코드")
    @NotBlank
    private String inviteCode;
}