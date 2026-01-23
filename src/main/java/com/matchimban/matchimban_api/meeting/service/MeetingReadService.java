package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.dto.MeetingDetailResponse;
import com.matchimban.matchimban_api.meeting.dto.MeetingParticipantSummary;
import com.matchimban.matchimban_api.meeting.dto.MyMeetingSummary;
import com.matchimban.matchimban_api.meeting.dto.MyMeetingsResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRow;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.meeting.repository.MyMeetingCursorRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingReadService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;

    public MyMeetingsResponse getMyMeetings(Long memberId, Long cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1);

        List<MyMeetingCursorRow> rows =
                meetingParticipantRepository.findMyMeetingCursorRows(memberId, cursor, pageable);

        boolean hasNext = rows.size() > size;
        List<MyMeetingCursorRow> pageRows = hasNext ? rows.subList(0, size) : rows;

        if (pageRows.isEmpty()) {
            return new MyMeetingsResponse(List.of(), null, false);
        }

        Long nextCursor = hasNext
                ? pageRows.get(pageRows.size() - 1).getMeetingParticipantId()
                : null;

        List<Long> meetingIds = pageRows.stream()
                .map(MyMeetingCursorRow::getMeetingId)
                .toList();

        List<Meeting> meetings = meetingRepository.findByIdIn(meetingIds);
        Map<Long, Meeting> meetingMap = meetings.stream()
                .collect(Collectors.toMap(Meeting::getId, Function.identity()));

        Map<Long, Long> countMap = meetingParticipantRepository.countActiveByMeetingIds(meetingIds).stream()
                .collect(Collectors.toMap(
                        MeetingParticipantRepository.MeetingCountRow::getMeetingId,
                        MeetingParticipantRepository.MeetingCountRow::getCnt
                ));

        List<MyMeetingSummary> items = new ArrayList<>(pageRows.size());
        for (MyMeetingCursorRow r : pageRows) {
            Meeting m = meetingMap.get(r.getMeetingId());
            if (m == null) continue;

            items.add(new MyMeetingSummary(
                    m.getId(),
                    m.getTitle(),
                    m.getScheduledAt(),
                    m.getLocationAddress(),
                    m.getTargetHeadcount(),
                    countMap.getOrDefault(m.getId(), 0L)
            ));
        }

        return new MyMeetingsResponse(items, nextCursor, hasNext);
    }

    public MeetingDetailResponse getMeetingDetail(Long memberId, Long meetingId) {
        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found", "meeting not found"));

        boolean isParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "not a meeting participant");
        }

        long participantCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );

        List<MeetingParticipantRow> rows = meetingParticipantRepository.findParticipantRows(meetingId);
        List<MeetingParticipantSummary> participants = rows.stream()
                .map(r -> new MeetingParticipantSummary(
                        r.getMemberId(),
                        r.getRole().name(),
                        r.getStatus().name(),
                        r.getCreatedAt()
                ))
                .toList();

        return new MeetingDetailResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getScheduledAt(),
                meeting.getVoteDeadlineAt(),
                meeting.getLocationAddress(),
                meeting.getLocationLat(),
                meeting.getLocationLng(),
                meeting.getTargetHeadcount(),
                meeting.getSearchRadiusM(),
                meeting.getSwipeCount(),
                meeting.isExceptMeat(),
                meeting.isExceptBar(),
                meeting.isQuickMeeting(),
                meeting.getInviteCode(),
                meeting.getHostMemberId(),
                participantCount,
                participants
        );
    }
}
