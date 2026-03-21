package com.matchimban.matchimban_api.event.controller;

import com.matchimban.matchimban_api.event.dto.request.EventListRequest;
import com.matchimban.matchimban_api.event.dto.response.EventListResponse;
import com.matchimban.matchimban_api.event.service.EventReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Event", description = "이벤트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventReadService eventReadService;

    @Operation(summary = "이벤트 목록 조회", description = "커서 기반 이벤트 목록 조회")
    @GetMapping
    public ResponseEntity<EventListResponse> getEvents(EventListRequest request) {
        return ResponseEntity.ok(eventReadService.getEvents(request));
    }
}