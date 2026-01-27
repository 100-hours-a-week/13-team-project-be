package com.matchimban.matchimban_api.meeting.repository;

import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.repository.projection.MeetingDetailRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    boolean existsByInviteCode(String inviteCode);

    Optional<Meeting> findByInviteCodeAndIsDeletedFalse(String inviteCode);

    Optional<Meeting> findByIdAndIsDeletedFalse(Long id);

    @Query("""
        select new com.matchimban.matchimban_api.meeting.repository.projection.MeetingDetailRow(
            m.id,
            m.title,
            m.scheduledAt,
            m.voteDeadlineAt,

            m.locationAddress,
            m.locationLat,
            m.locationLng,

            m.targetHeadcount,
            m.searchRadiusM,
            m.swipeCount,
            m.isExceptMeat,
            m.isExceptBar,
            m.isQuickMeeting,

            m.inviteCode,
            m.hostMemberId,

            (select count(mp.id)
               from MeetingParticipant mp
              where mp.meeting = m
                and mp.status = com.matchimban.matchimban_api.meeting.entity.MeetingParticipant.Status.ACTIVE
            ),

            (select v.id
               from Vote v
              where v.meeting = m
                and v.round = (select max(v2.round) from Vote v2 where v2.meeting = m)
            ),

            (select v.status
               from Vote v
              where v.meeting = m
                and v.round = (select max(v2.round) from Vote v2 where v2.meeting = m)
            ),

            (select (count(fs) > 0)
               from MeetingFinalSelection fs
              where fs.meeting.id = m.id
            )
        )
        from Meeting m
        where m.id = :meetingId
          and m.isDeleted = false
    """)
    Optional<MeetingDetailRow> findMeetingDetailRow(@Param("meetingId") Long meetingId);

}