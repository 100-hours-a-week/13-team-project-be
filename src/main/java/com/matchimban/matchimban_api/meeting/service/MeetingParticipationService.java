package com.matchimban.matchimban_api.meeting.service;

import com.matchimban.matchimban_api.global.error.ApiException;
import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingRequest;
import com.matchimban.matchimban_api.meeting.dto.ParticipateMeetingResponse;
import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import com.matchimban.matchimban_api.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingParticipationService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final EntityManager entityManager;

    @Transactional
    public ParticipateMeetingResponse participateMeeting(Long userId, ParticipateMeetingRequest request) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }

        Meeting meeting = meetingRepository.findByInviteCodeAndIsDeletedFalse(request.getInviteCode())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "meeting_not_found"));

        Long meetingId = meeting.getId();

        MeetingParticipant existing = meetingParticipantRepository
                .findByMeeting_IdAndUser_Id(meetingId, userId)
                .orElse(null);

        if (existing != null && existing.getStatus() == MeetingParticipant.Status.ACTIVE) {
            return new ParticipateMeetingResponse(meetingId);
        }

        long activeCount = meetingParticipantRepository.countByMeeting_IdAndStatus(
                meetingId, MeetingParticipant.Status.ACTIVE
        );

        if (activeCount >= meeting.getTargetHeadcount()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "meeting_full");
        }

        if (existing != null) {
            existing.reactivate();
            return new ParticipateMeetingResponse(meetingId);
        }

        User userRef = entityManager.getReference(User.class, userId);

        MeetingParticipant participant = MeetingParticipant.builder()
                .meeting(meeting)
                .user(userRef)
                .role(MeetingParticipant.Role.MEMBER)
                .status(MeetingParticipant.Status.ACTIVE)
                .build();

        meetingParticipantRepository.save(participant);
        return new ParticipateMeetingResponse(meetingId);
    }
}
