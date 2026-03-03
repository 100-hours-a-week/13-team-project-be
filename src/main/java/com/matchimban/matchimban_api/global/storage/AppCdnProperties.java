package com.matchimban.matchimban_api.global.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CDN(CloudFront) "public base url" 설정.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cdn")
public class AppCdnProperties {
    private String baseUrl = "";
}