package com.matchimban.matchimban_api.meeting.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.dto.response.QuickMeetingDetailResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.meeting.service.QuickMeetingReadService;
import com.matchimban.matchimban_api.vote.entity.Vote;
import com.matchimban.matchimban_api.vote.entity.enums.VoteStatus;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuickMeetingReadServiceImpl implements QuickMeetingReadService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final VoteRepository voteRepository;

    @Override
    @Transactional(readOnly = true)
    public QuickMeetingDetailResponse getQuickMeetingDetailByInviteCode(String inviteCode) {

        Meeting meeting = meetingRepository.findByInviteCodeAndIsDeletedFalse(inviteCode)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        if (!meeting.isQuickMeeting()) {
            throw new ApiException(MeetingErrorCode.NOT_QUICK_MEETING);
        }

        long participantCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meeting.getId(),
                MeetingParticipant.Status.ACTIVE
        );

        List<Vote> votes = voteRepository.findByMeetingIdOrderByRoundAsc(meeting.getId());
        Vote entry = resolveEntryVote(votes);

        Long currentVoteId = (entry == null) ? null : entry.getId();
        VoteStatus voteStatus = (entry == null) ? null : entry.getStatus();

        return new QuickMeetingDetailResponse(
                meeting.getId(),
                meeting.getInviteCode(),
                meeting.getLocationAddress(),
                participantCount,
                meeting.getTargetHeadcount(),
                meeting.getVoteDeadlineAt(),
                currentVoteId,
                voteStatus,
                meeting.getHostMemberId()
        );
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
}