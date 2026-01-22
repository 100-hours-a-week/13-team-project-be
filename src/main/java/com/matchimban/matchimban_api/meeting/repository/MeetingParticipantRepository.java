package com.matchimban.matchimban_api.meeting.repository;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    long countByMeeting_IdAndStatus(
            Long meetingId,
            MeetingParticipant.Status status
    );

    Optional<MeetingParticipant> findByMeeting_IdAndUser_Id(Long meetingId, Long userId);
}
