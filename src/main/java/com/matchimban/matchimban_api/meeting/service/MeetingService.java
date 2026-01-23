package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingResponse;
import com.matchimban.matchimban_api.meeting.dto.UpdateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.UpdateMeetingResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.member.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final int INVITE_CODE_LEN = 8;
    private static final int INVITE_CODE_RETRY = 10;
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final EntityManager entityManager;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CreateMeetingResponse createMeeting(Long memberId, CreateMeetingRequest req) {
        validateTimeRules(req.getScheduledAt(), req.getVoteDeadlineAt());

        for (int attempt = 1; attempt <= INVITE_CODE_RETRY; attempt++) {
            String inviteCode = generateInviteCode();

            if (meetingRepository.existsByInviteCode(inviteCode)) {
                continue;
            }

            try {
                Meeting meeting = Meeting.builder()
                        .title(req.getTitle())
                        .scheduledAt(req.getScheduledAt())
                        .voteDeadlineAt(req.getVoteDeadlineAt())
                        .locationAddress(req.getLocationAddress())
                        .locationLat(req.getLocationLat())
                        .locationLng(req.getLocationLng())
                        .searchRadiusM(req.getSearchRadiusM())
                        .targetHeadcount(req.getTargetHeadcount())
                        .swipeCount(req.getSwipeCount())
                        .isExceptMeat(req.isExceptMeat())
                        .isExceptBar(req.isExceptBar())
                        .isQuickMeeting(req.isQuickMeeting())
                        .inviteCode(inviteCode)
                        .hostMemberId(memberId)
                        .build();

                Meeting saved = meetingRepository.save(meeting);

                User memberRef = entityManager.getReference(User.class, memberId);
                MeetingParticipant host = MeetingParticipant.builder()
                        .meeting(saved)
                        .member(memberRef)
                        .role(MeetingParticipant.Role.HOST)
                        .status(MeetingParticipant.Status.ACTIVE)
                        .build();

                meetingParticipantRepository.save(host);

                return new CreateMeetingResponse(saved.getId(), saved.getInviteCode());

            } catch (DataIntegrityViolationException e) {
                if (attempt == INVITE_CODE_RETRY) {
                    throw new ApiException(HttpStatus.CONFLICT, "invite_code_conflict", e.getMessage());
                }
            }
        }

        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LEN);
        for (int i = 0; i < INVITE_CODE_LEN; i++) {
            int index = secureRandom.nextInt(INVITE_CODE_CHARS.length());
            sb.append(INVITE_CODE_CHARS.charAt(index));
        }
        return sb.toString();
    }

    @Transactional
    public void leaveMeeting(Long memberId, Long meetingId) {
        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found", "meeting not found"));

        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndMemberId(meetingId, memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "participant_not_found", "participant not found"));

        if (participant.getRole() == MeetingParticipant.Role.HOST || meeting.getHostMemberId().equals(memberId)) {
            throw new ApiException(HttpStatus.CONFLICT, "host_cannot_leave", "host cannot leave meeting");
        }

        if (participant.getStatus() == MeetingParticipant.Status.LEFT) {
            return;
        }

        participant.leave();
    }

    @Transactional
    public void deleteMeeting(Long memberId, Long meetingId) {
        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found", "meeting not found"));

        if (!meeting.getHostMemberId().equals(memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "only host can delete meeting");
        }

        meeting.delete();
    }

    @Transactional
    public UpdateMeetingResponse updateMeeting(Long memberId, Long meetingId, UpdateMeetingRequest req) {
        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found", "meeting not found"));

        if (!meeting.getHostMemberId().equals(memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "only host can update meeting");
        }

        LocalDateTime finalScheduledAt = (req.getScheduledAt() != null) ? req.getScheduledAt() : meeting.getScheduledAt();
        LocalDateTime finalVoteDeadlineAt = (req.getVoteDeadlineAt() != null) ? req.getVoteDeadlineAt() : meeting.getVoteDeadlineAt();
        validateTimeRules(finalScheduledAt, finalVoteDeadlineAt);

        meeting.update(
                req.getTitle(),
                req.getScheduledAt(),
                req.getVoteDeadlineAt(),
                req.getLocationAddress(),
                req.getLocationLat(),
                req.getLocationLng(),
                req.getTargetHeadcount(),
                req.getSearchRadiusM(),
                req.getSwipeCount(),
                req.getExceptMeat(),
                req.getExceptBar(),
                req.getQuickMeeting()
        );

        return new UpdateMeetingResponse(meeting.getId());
    }

    private void validateTimeRules(LocalDateTime scheduledAt, LocalDateTime voteDeadlineAt) {
        LocalDateTime now = LocalDateTime.now();

        if (scheduledAt.isBefore(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", "scheduled_at must not be in the past");
        }
        if (voteDeadlineAt.isBefore(now) || voteDeadlineAt.isAfter(scheduledAt)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request",
                    "vote_deadline_at must be between now and scheduled_at");
        }
    }

}