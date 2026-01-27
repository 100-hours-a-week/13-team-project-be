package com.matchimban.matchimban_api.meeting.service.serviceImpl;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.CreateMeetingResponse;
import com.matchimban.matchimban_api.meeting.dto.UpdateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.UpdateMeetingResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.meeting.service.MeetingService;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.vote.repository.VoteRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final VoteRepository voteRepository;
    private final EntityManager entityManager;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CreateMeetingResponse createMeeting(Long memberId, CreateMeetingRequest req) {
        validateTimeRules(req.getScheduledAt(), req.getVoteDeadlineAt());

        int inviteCodeRetry = 10;
        for (int attempt = 1; attempt <= inviteCodeRetry; attempt++) {
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

                Member memberRef = entityManager.getReference(Member.class, memberId);
                MeetingParticipant host = MeetingParticipant.builder()
                        .meeting(saved)
                        .member(memberRef)
                        .role(MeetingParticipant.Role.HOST)
                        .status(MeetingParticipant.Status.ACTIVE)
                        .build();

                meetingParticipantRepository.save(host);

                return new CreateMeetingResponse(saved.getId(), saved.getInviteCode());

            } catch (DataIntegrityViolationException e) {
                if (attempt == inviteCodeRetry) {
                    throw new ApiException(HttpStatus.CONFLICT, "invite_code_conflict", e.getMessage());
                }
            }
        }

        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");
    }

    @Transactional
    public UpdateMeetingResponse updateMeeting(Long memberId, Long meetingId, UpdateMeetingRequest req) {
        Meeting meeting = meetingRepository.findByIdAndIsDeletedFalse(meetingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found", "meeting not found"));

        if (!meeting.getHostMemberId().equals(memberId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "only host can update meeting");
        }

        validateUpdateNotAllowedAfterVoteCreated(meetingId);

        String finalTitle = (req.getTitle() != null) ? req.getTitle() : meeting.getTitle();
        LocalDateTime finalScheduledAt = (req.getScheduledAt() != null) ? req.getScheduledAt() : meeting.getScheduledAt();
        LocalDateTime finalVoteDeadlineAt = (req.getVoteDeadlineAt() != null) ? req.getVoteDeadlineAt() : meeting.getVoteDeadlineAt();

        String finalLocationAddress = (req.getLocationAddress() != null) ? req.getLocationAddress() : meeting.getLocationAddress();
        BigDecimal finalLat = (req.getLocationLat() != null) ? req.getLocationLat() : meeting.getLocationLat();
        BigDecimal finalLng = (req.getLocationLng() != null) ? req.getLocationLng() : meeting.getLocationLng();

        Integer finalTargetHeadcount = (req.getTargetHeadcount() != null) ? req.getTargetHeadcount() : meeting.getTargetHeadcount();
        Integer finalSearchRadiusM = (req.getSearchRadiusM() != null) ? req.getSearchRadiusM() : meeting.getSearchRadiusM();
        Integer finalSwipeCount = (req.getSwipeCount() != null) ? req.getSwipeCount() : meeting.getSwipeCount();

        Boolean finalExceptMeat = (req.getExceptMeat() != null) ? req.getExceptMeat() : meeting.isExceptMeat();
        Boolean finalExceptBar = (req.getExceptBar() != null) ? req.getExceptBar() : meeting.isExceptBar();
        Boolean finalQuickMeeting = (req.getQuickMeeting() != null) ? req.getQuickMeeting() : meeting.isQuickMeeting();

        validateTimeRules(finalScheduledAt, finalVoteDeadlineAt);

        meeting.update(
                finalTitle,
                finalScheduledAt,
                finalVoteDeadlineAt,
                finalLocationAddress,
                finalLat,
                finalLng,
                finalTargetHeadcount,
                finalSearchRadiusM,
                finalSwipeCount,
                finalExceptMeat,
                finalExceptBar,
                finalQuickMeeting
        );

        return new UpdateMeetingResponse(meeting.getId());
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

    private String generateInviteCode() {
        int inviteCodeLen = 8;
        String inviteCodeChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        StringBuilder sb = new StringBuilder(inviteCodeLen);
        for (int i = 0; i < inviteCodeLen; i++) {
            int index = secureRandom.nextInt(inviteCodeChars.length());
            sb.append(inviteCodeChars.charAt(index));
        }
        return sb.toString();
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

    private void validateUpdateNotAllowedAfterVoteCreated(Long meetingId) {
        if (voteRepository.existsByMeetingId(meetingId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "meeting_update_not_allowed",
                    "cannot update meeting after vote has been created"
            );
        }
    }

}