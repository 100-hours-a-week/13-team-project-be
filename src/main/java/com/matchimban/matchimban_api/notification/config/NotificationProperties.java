package com.matchimban.matchimban_api.notification.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private final Dispatch dispatch = new Dispatch();
    private final Schedule schedule = new Schedule();
    private final Retry retry = new Retry();
    private final Retention retention = new Retention();
    private final Firebase firebase = new Firebase();

    @Getter
    @Setter
    public static class Dispatch {
        private boolean enabled = true;
        private Duration pollDelay = Duration.ofSeconds(2);
        private int batchSize = 20;
        private Duration staleLockThreshold = Duration.ofMinutes(2);
    }

    @Getter
    @Setter
    public static class Schedule {
        private boolean enabled = true;
        private Duration pollDelay = Duration.ofSeconds(2);
        private int batchSize = 20;
        private Duration staleLockThreshold = Duration.ofMinutes(2);
    }

    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 6;
    }

    @Getter
    @Setter
    public static class Retention {
        private boolean enabled = true;
        private Duration notificationDeletedRetention = Duration.ofDays(30);
        private Duration notificationCreatedRetention = Duration.ofDays(180);
        private Duration outboxRetention = Duration.ofDays(14);
        private Duration scheduleRetention = Duration.ofDays(14);
        private Duration inactiveTokenRetention = Duration.ofDays(90);
    }

    @Getter
    @Setter
    public static class Firebase {
        private boolean enabled = false;
        private String projectId = "moyeobab";
        private String serviceAccountJsonBase64;
        private String serviceAccountFile;
    }
}
