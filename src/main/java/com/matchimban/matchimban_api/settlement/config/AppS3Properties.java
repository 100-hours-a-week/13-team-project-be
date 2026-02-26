package com.matchimban.matchimban_api.settlement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.s3")
public class AppS3Properties {
    private String bucket;
    private String receiptPrefix = "settlements";
    private Duration presignUploadTtl = Duration.ofMinutes(10);
}