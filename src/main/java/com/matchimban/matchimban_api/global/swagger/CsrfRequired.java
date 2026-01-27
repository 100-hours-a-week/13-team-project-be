package com.matchimban.matchimban_api.global.swagger;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
	name = "X-CSRF-Token",
	in = ParameterIn.HEADER,
	required = true,
	description = "CSRF token from csrf_token cookie. Use withCredentials: true so cookies are sent."
)
public @interface CsrfRequired {
}
