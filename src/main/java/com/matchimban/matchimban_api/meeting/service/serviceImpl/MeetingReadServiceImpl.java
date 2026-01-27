package com.matchimban.matchimban_api.meeting.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.dto.*;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.*;
import com.matchimban.matchimban_api.meeting.repository.projection.MeetingDetailRow;
import com.matchimban.matchimban_api.meeting.repository.projection.MyMeetingRow;
import com.matchimban.matchimban_api.meeting.service.MeetingReadService;
import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingReadServiceImpl implements MeetingReadService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;

    public MyMeetingsResponse getMyMeetings(Long memberId, Long cursor, int size) {

        Pageable pageable = PageRequest.of(0, size + 1);

        List<MyMeetingRow> rows = meetingParticipantRepository.findMyMeetingRows(
                memberId,
                cursor,
                MeetingParticipant.Status.ACTIVE,
                pageable
        );

        boolean hasNext = rows.size() > size;
        List<MyMeetingRow> pageRows = hasNext ? rows.subList(0, size) : rows;

        if (pageRows.isEmpty()) {
            return new MyMeetingsResponse(List.of(), null, false);
        }

        Long nextCursor = hasNext
                ? pageRows.get(pageRows.size() - 1).getMeetingParticipantId()
                : null;

        List<MyMeetingSummary> items = pageRows.stream()
                .map(r -> new MyMeetingSummary(
                        r.getMeetingId(),
                        r.getTitle(),
                        r.getScheduledAt(),
                        r.getParticipantCount(),
                        r.getTargetHeadcount(),
                        mapMeetingStatus(r.getVoteStatus())
                ))
                .toList();

        return new MyMeetingsResponse(items, nextCursor, hasNext);
    }

    private MeetingStatus mapMeetingStatus(VoteStatus voteStatus) {
        if (voteStatus == null) return MeetingStatus.READY;

        return switch (voteStatus) {
            case GENERATING -> MeetingStatus.READY;
            case OPEN, COUNTING -> MeetingStatus.VOTING;
            case COUNTED -> MeetingStatus.DONE;
            case FAILED -> MeetingStatus.READY; // 실패는 완료가 아니므로 READY로 환원(재시도 UX)
        };
    }

    public MeetingDetailResponse getMeetingDetail(Long memberId, Long meetingId) {

        boolean isActiveParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId,
                memberId,
                MeetingParticipant.Status.ACTIVE
        );
        if (!isActiveParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "not an active participant of this meeting");
        }

        MeetingDetailRow row = meetingRepository.findMeetingDetailRow(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found", "meeting not found"));

        List<MeetingParticipantSummary> participants = meetingParticipantRepository
                .findActiveParticipantProfiles(meetingId)
                .stream()
                .map(p -> new MeetingParticipantSummary(
                        p.getMemberId(),
                        p.getNickname(),
                        p.getProfileImageUrl()
                ))
                .toList();

        VoteStatus voteState = row.getVoteState();
        MeetingStatus meetingStatus = mapMeetingStatus(voteState);

        return new MeetingDetailResponse(
                row.getMeetingId(),
                row.getTitle(),
                row.getScheduledAt(),
                row.getVoteDeadlineAt(),
                row.getLocationAddress(),
                row.getLocationLat(),
                row.getLocationLng(),
                row.getTargetHeadcount(),
                row.getSearchRadiusM(),
                row.getSwipeCount(),
                row.isExceptMeat(),
                row.isExceptBar(),
                row.isQuickMeeting(),
                row.getInviteCode(),
                row.getHostMemberId(),
                row.getParticipantCount(),
                participants,
                row.getCurrentVoteId(),
                voteState,
                row.isFinalSelected(),
                meetingStatus
        );
    }

}
