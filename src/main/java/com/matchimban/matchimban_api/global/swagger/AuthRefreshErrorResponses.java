package com.matchimban.matchimban_api.global.swagger;

import com.matchimban.matchimban_api.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
	@ApiResponse(
		responseCode = "401",
		description = "invalid_refresh_token / invalid_access_token",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class))
	),
	@ApiResponse(
		responseCode = "403",
		description = "account_deleted",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class))
	),
	@ApiResponse(
		responseCode = "500",
		description = "internal_server_error",
		content = @Content(schema = @Schema(implementation = ErrorResponse.class))
	)
})
public @interface AuthRefreshErrorResponses {
}
