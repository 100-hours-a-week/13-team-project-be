package com.matchimban.matchimban_api.global.aws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3PresignerConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Presigner.class)
    public S3Presigner s3Presigner(AwsCredentialsProvider credentialsProvider,
                                   AwsRegionProvider regionProvider) {
        return S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(regionProvider.getRegion())
                .build();
    }
}