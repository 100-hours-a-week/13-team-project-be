package com.matchimban.matchimban_api.notification.scheduler;

import com.matchimban.matchimban_api.notification.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationDispatchScheduler {

    private final NotificationDispatchService notificationDispatchService;

    @Scheduled(fixedDelayString = "${notification.dispatch.poll-delay:2000ms}")
    public void tick() {
        notificationDispatchService.tick();
    }
}
