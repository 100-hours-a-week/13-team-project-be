package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.user.entity.User;
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
    public CreateMeetingResponse createMeeting(Long userId, CreateMeetingRequest req) {
        validateTimeRules(req);

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
                        .hostUserId(userId)
                        .build();

                Meeting saved = meetingRepository.save(meeting);

                User userRef = entityManager.getReference(User.class, userId);
                MeetingParticipant host = MeetingParticipant.builder()
                        .meeting(saved)
                        .user(userRef)
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

    private void validateTimeRules(CreateMeetingRequest req) {
        LocalDateTime now = LocalDateTime.now();

        if (req.getScheduledAt().isBefore(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_request", "scheduled_at must not be in the past");
        }

        if (req.getVoteDeadlineAt().isBefore(now) || req.getVoteDeadlineAt().isAfter(req.getScheduledAt())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_request",
                    "vote_deadline_at must be between now and scheduled_at"
            );
        }
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LEN);
        for (int i = 0; i < INVITE_CODE_LEN; i++) {
            int index = secureRandom.nextInt(INVITE_CODE_CHARS.length());
            sb.append(INVITE_CODE_CHARS.charAt(index));
        }
        return sb.toString();
    }
}