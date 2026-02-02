package com.matchimban.matchimban_api.meeting.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.dto.*;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.meeting.repository.projection.MeetingDetailRow;
import com.matchimban.matchimban_api.meeting.repository.projection.MyMeetingRow;
import com.matchimban.matchimban_api.meeting.service.MeetingReadService;
import com.matchimban.matchimban_api.vote.entity.Vote;
import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import com.matchimban.matchimban_api.vote.repository.VoteSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingReadServiceImpl implements MeetingReadService {

    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingRepository meetingRepository;
    private final VoteRepository voteRepository;
    private final VoteSubmissionRepository voteSubmissionRepository;

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
            case GENERATING, RESERVED -> MeetingStatus.READY;
            case OPEN, COUNTING -> MeetingStatus.VOTING;
            case COUNTED -> MeetingStatus.DONE;
            case FAILED -> MeetingStatus.READY;
        };
    }

    private Vote resolveEntryVote(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) return null;

        Comparator<Vote> byRoundAsc = Comparator.comparingInt(Vote::getRound);

        Vote v1 = votes.stream()
                .filter(v -> v.getRound() == 1)
                .findFirst()
                .orElse(null);

        Vote open = votes.stream()
                .filter(v -> v.getStatus() == VoteStatus.OPEN)
                .max(byRoundAsc)
                .orElse(null);
        if (open != null) return open;

        Vote counting = votes.stream()
                .filter(v -> v.getStatus() == VoteStatus.COUNTING)
                .max(byRoundAsc)
                .orElse(null);
        if (counting != null) return counting;

        if (v1 != null && (v1.getStatus() == VoteStatus.GENERATING || v1.getStatus() == VoteStatus.FAILED)) {
            return v1;
        }

        Vote counted = votes.stream()
                .filter(v -> v.getStatus() == VoteStatus.COUNTED)
                .max(byRoundAsc)
                .orElse(null);
        if (counted != null) return counted;

        return votes.stream()
                .filter(v -> v.getStatus() != VoteStatus.RESERVED)
                .max(byRoundAsc)
                .orElse(v1);
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

        List<Vote> votes = voteRepository.findByMeetingIdOrderByRoundAsc(meetingId);
        Vote entryVote = resolveEntryVote(votes);

        Long currentVoteId = (entryVote == null) ? null : entryVote.getId();
        VoteStatus voteState = (entryVote == null) ? null : entryVote.getStatus();
        MeetingStatus meetingStatus = mapMeetingStatus(voteState);

        boolean hasVotedCurrent = (currentVoteId != null)
                && voteSubmissionRepository.existsByVoteIdAndParticipantMemberId(currentVoteId, memberId);

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
                currentVoteId,
                voteState,
                hasVotedCurrent,
                row.isFinalSelected(),
                meetingStatus
        );
    }

    @Transactional(readOnly = true)
    public MeetingDetailStateResponse getMeetingDetailState(Long memberId, Long meetingId) {

        boolean isActiveParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActiveParticipant) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "not an active participant of this meeting");
        }

        MeetingDetailRow row = meetingRepository.findMeetingDetailRow(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found", "meeting not found"));

        List<Vote> votes = voteRepository.findByMeetingIdOrderByRoundAsc(meetingId);
        Vote entryVote = resolveEntryVote(votes);

        Long currentVoteId = (entryVote == null) ? null : entryVote.getId();
        VoteStatus voteState = (entryVote == null) ? null : entryVote.getStatus();
        MeetingStatus meetingStatus = mapMeetingStatus(voteState);

        boolean hasVotedCurrent = (currentVoteId != null)
                && voteSubmissionRepository.existsByVoteIdAndParticipantMemberId(currentVoteId, memberId);

        long participantCount = row.getParticipantCount();

        List<MeetingParticipantSummary> participants = null;
        if (meetingStatus == MeetingStatus.READY) {
            participants = meetingParticipantRepository
                    .findActiveParticipantProfiles(meetingId)
                    .stream()
                    .map(p -> new MeetingParticipantSummary(
                            p.getMemberId(),
                            p.getNickname(),
                            p.getProfileImageUrl()
                    ))
                    .toList();
        }

        return new MeetingDetailStateResponse(
                participantCount,
                currentVoteId,
                voteState,
                hasVotedCurrent,
                row.isFinalSelected(),
                meetingStatus,
                participants
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InviteCodeResponse getInviteCode(Long memberId, Long meetingId) {

        boolean isActive = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
                meetingId, memberId, MeetingParticipant.Status.ACTIVE
        );
        if (!isActive) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden_not_active_participant");
        }

        String inviteCode = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .map(m -> m.getInviteCode())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found"));

        return new InviteCodeResponse(inviteCode);
    }
}
