package com.matchimban.matchimban_api.settlement.ocr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ocr")
public class AppOcrProperties {
    private String baseUrl;
    private Duration timeout = Duration.ofSeconds(30);
    private Duration healthTimeout = Duration.ofSeconds(3);
    private Duration lease = Duration.ofSeconds(60);
    private Duration pollDelay = Duration.ofMillis(1000);
    private Duration initialRetryDelay = Duration.ofSeconds(10);
    private Duration maxRetryDelay = Duration.ofSeconds(60);
    private int maxAttempts = 3;
}