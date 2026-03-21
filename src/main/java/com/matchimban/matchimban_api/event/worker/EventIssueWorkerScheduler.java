package com.matchimban.matchimban_api.event.worker;

import com.matchimban.matchimban_api.event.config.EventIssueProperties;
import com.matchimban.matchimban_api.event.service.EventIssueWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventIssueWorkerScheduler {

    private final EventIssueProperties properties;
    private final EventIssueWorkerService eventIssueWorkerService;

    @Scheduled(fixedDelayString = "${event.issue.poll-delay:200ms}")
    public void processBatch() {
        if (!properties.enabled()) {
            return;
        }
        try {
            eventIssueWorkerService.processBatchForActiveEvents();
        } catch (Exception ex) {
            log.error("Failed to process event issue worker batch", ex);
        }
    }
}
