package com.matchimban.matchimban_api.meeting.service.serviceImpl;

import com.matchimban.matchimban_api.chat.service.ChatSystemMessageService;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.dto.request.ParticipateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.response.ParticipateMeetingResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.error.MeetingErrorCode;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.meeting.service.MeetingParticipationService;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.event.NotificationRequestedEvent;
import com.matchimban.matchimban_api.settlement.enums.SettlementStatus;
import com.matchimban.matchimban_api.settlement.repository.MeetingSettlementRepository;
import com.matchimban.matchimban_api.vote.entity.enums.VoteStatus;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingParticipationServiceImpl implements MeetingParticipationService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final VoteRepository voteRepository;
    private final ChatSystemMessageService chatSystemMessageService;
    private final EntityManager entityManager;
    private final MeetingSettlementRepository meetingSettlementRepository;
    private final ApplicationEventPublisher eventPublisher;

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
            publishMemberJoinedNotification(meetingId, existing.getId(), memberId, existing.getMember().getNickname());
            chatSystemMessageService.publishSystemMessage(existing, buildJoinSystemMessage(existing.getMember().getNickname()));
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
        publishMemberJoinedNotification(meetingId, participant.getId(), memberId, memberRef.getNickname());
        chatSystemMessageService.publishSystemMessage(participant, buildJoinSystemMessage(memberRef.getNickname()));
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
        validateLeaveAllowedBySettlementState(meetingId);

        participant.leave();
        chatSystemMessageService.publishSystemMessage(participant, buildLeaveSystemMessage(participant.getMember().getNickname()));
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

    private void validateLeaveAllowedBySettlementState(Long meetingId) {
        meetingSettlementRepository.findByMeetingId(meetingId)
                .ifPresent(settlement -> {
                    SettlementStatus status = settlement.getSettlementStatus();
                    if (status == SettlementStatus.SELECTION_OPEN
                            || status == SettlementStatus.CALCULATING) {
                        throw new ApiException(MeetingErrorCode.SETTLEMENT_IN_PROGRESS);
                    }
                });
    }

    private String buildJoinSystemMessage(String nickname) {
        return safeNickname(nickname) + "님이 들어왔습니다.";
    }

    private String buildLeaveSystemMessage(String nickname) {
        return safeNickname(nickname) + "님이 나갔습니다.";
    }

    private String safeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "사용자";
        }
        return nickname;
    }

    private void publishMemberJoinedNotification(Long meetingId, Long participantId, Long joinedMemberId, String nickname) {
        List<Long> recipients = meetingParticipantRepository.findActiveMemberIds(meetingId).stream()
                .filter(memberId -> !memberId.equals(joinedMemberId))
                .toList();

        if (recipients.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(new NotificationRequestedEvent(
                NotificationType.MEETING_MEMBER_JOINED,
                "모임 참여 알림",
                safeNickname(nickname) + "님이 모임에 참여했어요.",
                "MEETING",
                meetingId,
                participantId,
                "/meetings/" + meetingId,
                "MEETING_MEMBER_JOINED:" + meetingId + ":" + participantId,
                null,
                recipients
        ));
    }
}
