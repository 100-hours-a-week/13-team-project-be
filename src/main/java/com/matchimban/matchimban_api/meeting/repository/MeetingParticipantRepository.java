package com.matchimban.matchimban_api.meeting.repository;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    @Query("""
        select count(mp)
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.status = :status
    """)
    long countByMeetingIdAndStatus(
            @Param("meetingId") Long meetingId,
            @Param("status") MeetingParticipant.Status status
    );

    @Query("""
        select mp
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.user.id = :userId
    """)
    Optional<MeetingParticipant> findByMeetingIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

}
