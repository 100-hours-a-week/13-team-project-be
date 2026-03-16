package com.matchimban.matchimban_api.notification.service;

import java.time.Duration;

public final class NotificationRetryPolicy {

    private NotificationRetryPolicy() {
    }

    public static Duration backoffForAttempt(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> Duration.ofSeconds(10);
            case 2 -> Duration.ofSeconds(30);
            case 3 -> Duration.ofMinutes(2);
            case 4 -> Duration.ofMinutes(10);
            case 5 -> Duration.ofMinutes(30);
            default -> null;
        };
    }
}
