package com.matchimban.matchimban_api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AsyncApiResourceConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/asyncapi/**")
			.addResourceLocations(
				"file:docs/asyncapi/",
				"classpath:/static/asyncapi/"
			);
	}
}
