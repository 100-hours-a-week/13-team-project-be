package com.matchimban.matchimban_api.global.error.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FieldErrorData")
public record FieldErrorData (
        @Schema(example = "inviteCode") String field,
        @Schema(example = "초대코드가 필요합니다.") String reason) {
}
