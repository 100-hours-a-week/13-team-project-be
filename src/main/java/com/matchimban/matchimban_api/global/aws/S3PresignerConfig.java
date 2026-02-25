package com.matchimban.matchimban_api.global.aws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Profile("!test")
public class S3PresignerConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(S3Presigner.class)
    public S3Presigner s3Presigner(
            @Value("${spring.cloud.aws.region.static:ap-northeast-2}") String region
    ) {
        return S3Presigner.builder()
                // Default chain is resolved lazily at call time, so startup doesn't fail in CI test env.
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(region))
                .build();
    }
}
