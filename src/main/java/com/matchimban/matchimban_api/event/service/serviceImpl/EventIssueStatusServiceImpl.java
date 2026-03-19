package com.matchimban.matchimban_api.event.service.serviceImpl;

import com.matchimban.matchimban_api.event.dto.response.EventIssueStatusResponse;
import com.matchimban.matchimban_api.event.error.EventErrorCode;
import com.matchimban.matchimban_api.event.redis.EventIssueRedisRepository;
import com.matchimban.matchimban_api.event.service.EventIssueStatusService;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventIssueStatusServiceImpl implements EventIssueStatusService {

    private final EventIssueRedisRepository eventIssueRedisRepository;

    @Override
    public EventIssueStatusResponse getCurrentStatus(Long memberId, Long eventId) {
        String requestId = eventIssueRedisRepository.getCurrentRequestId(eventId, memberId);
        if (requestId == null || requestId.isBlank()) {
            throw new ApiException(EventErrorCode.EVENT_ISSUE_REQUEST_NOT_FOUND);
        }

        Map<Object, Object> requestData = eventIssueRedisRepository.getRequest(eventId, requestId);
        if (requestData.isEmpty()) {
            throw new ApiException(EventErrorCode.EVENT_ISSUE_REQUEST_NOT_FOUND);
        }

        return eventIssueRedisRepository.toStatusResponse(eventId, requestId, requestData);
    }
}
