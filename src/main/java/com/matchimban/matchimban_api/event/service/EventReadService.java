package com.matchimban.matchimban_api.event.service;

import com.matchimban.matchimban_api.event.dto.request.EventListRequest;
import com.matchimban.matchimban_api.event.dto.response.EventListResponse;

public interface EventReadService {

    EventListResponse getEvents(EventListRequest request);
}