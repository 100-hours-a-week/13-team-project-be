package com.matchimban.matchimban_api.meeting.repository;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRow;
import com.matchimban.matchimban_api.meeting.repository.MyMeetingCursorRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    @Query("""
        select (count(mp) > 0)
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.member.id = :memberId
          and mp.status = :status
    """)
    boolean existsByMeetingIdAndMemberIdAndStatus(
            @Param("meetingId") Long meetingId,
            @Param("memberId") Long memberId,
            @Param("status") MeetingParticipant.Status status
    );

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
          and mp.member.id = :memberId
    """)
    Optional<MeetingParticipant> findByMeetingIdAndMemberId(
            @Param("meetingId") Long meetingId,
            @Param("memberId") Long memberId
    );

    @Query("""
    select 
        mp.member.id as memberId,
        mp.role as role,
        mp.status as status,
        mp.createdAt as createdAt
    from MeetingParticipant mp
    where mp.meeting.id = :meetingId
    order by mp.createdAt asc, mp.id asc
""")
    List<MeetingParticipantRow> findParticipantRows(@Param("meetingId") Long meetingId);

    interface MeetingCountRow {
        Long getMeetingId();
        Long getCnt();
    }

    @Query("""
        select mp.meeting.id as meetingId, count(mp) as cnt
        from MeetingParticipant mp
        where mp.meeting.id in :meetingIds
          and mp.status = com.matchimban.matchimban_api.meeting.entity.MeetingParticipant.Status.ACTIVE
        group by mp.meeting.id
    """)
    List<MeetingCountRow> countActiveByMeetingIds(@Param("meetingIds") List<Long> meetingIds);

    @Query("""
    select 
        mp.id as meetingParticipantId,
        m.id as meetingId
    from MeetingParticipant mp
    join mp.meeting m
    where mp.member.id = :memberId
      and mp.status = com.matchimban.matchimban_api.meeting.entity.MeetingParticipant.Status.ACTIVE
      and m.isDeleted = false
      and (:cursor is null or mp.id < :cursor)
    order by mp.id desc
""")
    List<MyMeetingCursorRow> findMyMeetingCursorRows(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

}
