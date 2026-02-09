package com.matchimban.matchimban_api.global.error.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ErrorResponse")
public record ErrorResponse(
	@Schema(example = "INVALID_JSON") String code,
	@Schema(example = "요청 바디 형식이 올바르지 않습니다.") String message,
    @Schema(description = "Validation 실패 시 주로 사용") List<FieldErrorData> errors,
    @Schema(description = "비즈니스 에러에서 추가 정보 필요 시 사용") Object data
) { }
