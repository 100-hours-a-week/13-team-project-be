package com.matchimban.matchimban_api.global.swagger;

import com.matchimban.matchimban_api.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
	responseCode = "500",
	description = "internal_server_error",
	content = @Content(schema = @Schema(implementation = ErrorResponse.class))
)
public @interface InternalServerErrorResponse {
}
