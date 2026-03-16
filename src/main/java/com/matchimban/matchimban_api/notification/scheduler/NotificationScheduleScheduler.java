package com.matchimban.matchimban_api.notification.scheduler;

import com.matchimban.matchimban_api.notification.service.NotificationScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationScheduleScheduler {

    private final NotificationScheduleService notificationScheduleService;

    @Scheduled(fixedDelayString = "${notification.schedule.poll-delay:2000ms}")
    public void tick() {
        notificationScheduleService.tick();
    }

    @Scheduled(cron = "${notification.retention.cleanup-cron:0 20 4 * * *}")
    public void cleanupRetention() {
        notificationScheduleService.cleanupRetention();
    }
}
