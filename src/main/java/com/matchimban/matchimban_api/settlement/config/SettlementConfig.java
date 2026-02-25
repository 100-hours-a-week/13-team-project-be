package com.matchimban.matchimban_api.settlement.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppS3Properties.class)
public class SettlementConfig {
}