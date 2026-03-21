package com.matchimban.matchimban_api.event.service.serviceImpl;

import com.matchimban.matchimban_api.event.dto.request.EventListRequest;
import com.matchimban.matchimban_api.event.dto.response.EventListResponse;
import com.matchimban.matchimban_api.event.dto.response.EventSummaryResponse;
import com.matchimban.matchimban_api.event.entity.Event;
import com.matchimban.matchimban_api.event.repository.EventRepository;
import com.matchimban.matchimban_api.event.service.EventReadService;
import com.matchimban.matchimban_api.event.support.EventProgressStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventReadServiceImpl implements EventReadService {

    private final EventRepository eventRepository;
    private final EventProgressStatusResolver eventProgressStatusResolver;

    @Override
    public EventListResponse getEvents(EventListRequest request) {
        Instant now = Instant.now();
        int size = request.getNormalizedSize();

        List<Event> events = eventRepository.findEventPage(
                request.getCursorCreatedAt(),
                request.getCursorId(),
                size + 1
        );

        boolean hasNext = events.size() > size;
        List<Event> pageItems = hasNext ? events.subList(0, size) : events;

        List<EventSummaryResponse> items = pageItems.stream()
                .map(event -> toSummary(event, now))
                .toList();

        Instant nextCursorCreatedAt = null;
        Long nextCursorId = null;

        if (hasNext && !pageItems.isEmpty()) {
            Event last = pageItems.get(pageItems.size() - 1);
            nextCursorCreatedAt = last.getCreatedAt();
            nextCursorId = last.getId();
        }

        return new EventListResponse(items, hasNext, nextCursorCreatedAt, nextCursorId);
    }

    private EventSummaryResponse toSummary(Event event, Instant now) {
        int remainingCount = eventProgressStatusResolver.calculateRemainingCount(event);

        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCouponType(),
                eventProgressStatusResolver.resolve(event, now),
                event.getStartAt(),
                event.getEndAt(),
                event.getCapacity(),
                event.getIssuedCount(),
                remainingCount
        );
    }
}
