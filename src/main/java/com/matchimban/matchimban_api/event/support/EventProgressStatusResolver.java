package com.matchimban.matchimban_api.event.support;

import com.matchimban.matchimban_api.event.entity.Event;
import com.matchimban.matchimban_api.event.entity.EventProgressStatus;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class EventProgressStatusResolver {

    public EventProgressStatus resolve(Event event, Instant now) {
        if (now.isBefore(event.getStartAt())) {
            return EventProgressStatus.SCHEDULED;
        }
        if (!now.isBefore(event.getEndAt())) {
            return EventProgressStatus.ENDED;
        }
        if (event.getIssuedCount() >= event.getCapacity()) {
            return EventProgressStatus.SOLD_OUT;
        }
        return EventProgressStatus.IN_PROGRESS;
    }

    public int calculateRemainingCount(Event event) {
        return Math.max(0, event.getCapacity() - event.getIssuedCount());
    }
}
