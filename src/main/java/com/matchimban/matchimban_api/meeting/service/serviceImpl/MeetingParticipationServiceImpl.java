package com.matchimban.matchimban_api.meeting.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.dto.MeetingParticipantSummary;
import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.meeting.service.MeetingParticipationService;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.vote.entity.VoteStatus;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingParticipationServiceImpl implements MeetingParticipationService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final VoteRepository voteRepository;
    private final EntityManager entityManager;

    @Transactional
    public ParticipateMeetingResponse participateMeeting(Long memberId, ParticipateMeetingRequest request) {

        Meeting meeting = meetingRepository.findByInviteCodeAndIsDeletedFalse(request.getInviteCode())
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        Long meetingId = meeting.getId();

        MeetingParticipant existing = meetingParticipantRepository
                .findByMeetingIdAndMemberId(meetingId, memberId)
                .orElse(null);

        if (existing != null && existing.getStatus() == MeetingParticipant.Status.ACTIVE) {
            return new ParticipateMeetingResponse(meetingId);
        }

        long activeCount = meetingParticipantRepository.countByMeetingIdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );

        if (activeCount >= meeting.getTargetHeadcount()) {
            throw new ApiException(MeetingErrorCode.MEETING_FULL);
        }

        if (existing != null) {
            existing.reactivate();
            return new ParticipateMeetingResponse(meetingId);
        }

        Member memberRef = entityManager.getReference(Member.class, memberId);

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .member(memberRef)
                .role(MeetingParticipant.Role.MEMBER)
                .status(MeetingParticipant.Status.ACTIVE)
                .build();

        meetingParticipantRepository.save(participant);
        return new ParticipateMeetingResponse(meetingId);
    }

    @Transactional
    public void leaveMeeting(Long memberId, Long meetingId) {
        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.MEETING_NOT_FOUND));

        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndMemberId(meetingId, memberId)
                .orElseThrow(() -> new ApiException(MeetingErrorCode.PARTICIPANT_NOT_FOUND));

        if (participant.getRole() == MeetingParticipant.Role.HOST || meeting.getHostMemberId().equals(memberId)) {
            throw new ApiException(MeetingErrorCode.HOST_CANNOT_LEAVE);
        }

        if (participant.getStatus() == MeetingParticipant.Status.LEFT) {
            return;
        }

        validateLeaveAllowedByVoteState(meetingId);

        participant.leave();
    }

    private void validateLeaveAllowedByVoteState(Long meetingId) {
        voteRepository.findTopByMeetingIdOrderByRoundDesc(meetingId)
                .ifPresent(vote -> {
                    VoteStatus state = vote.getStatus();
                    if (state == VoteStatus.GENERATING
                            || state == VoteStatus.OPEN
                            || state == VoteStatus.COUNTING) {
                        throw new ApiException(MeetingErrorCode.VOTE_IN_PROGRESS);
                    }
                });
    }
}