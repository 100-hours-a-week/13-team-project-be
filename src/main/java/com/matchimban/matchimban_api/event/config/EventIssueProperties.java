package com.matchimban.matchimban_api.event.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "event.issue")
public record EventIssueProperties(
        boolean enabled,
        Duration pollDelay,
        int batchSizePerEvent,
        int maxFinalizeRetries,
        Duration processingLease,
        Duration terminalStatusTtl,
        long pollingIntervalMillis
) {
}
