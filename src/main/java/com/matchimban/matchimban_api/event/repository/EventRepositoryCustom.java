package com.matchimban.matchimban_api.event.repository;

import com.matchimban.matchimban_api.event.entity.Event;

import java.time.Instant;
import java.util.List;

public interface EventRepositoryCustom {

    List<Event> findEventPage(Instant cursorCreatedAt, Long cursorId, int size);
}