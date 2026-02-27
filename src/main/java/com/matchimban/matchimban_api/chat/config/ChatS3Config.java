package com.matchimban.matchimban_api.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class ChatS3Config {

	@Bean
	public S3Presigner s3Presigner(
		@Value("${spring.cloud.aws.region.static:ap-northeast-2}") String awsRegion
	) {
		return S3Presigner.builder()
			.region(Region.of(awsRegion))
			.build();
	}
}

