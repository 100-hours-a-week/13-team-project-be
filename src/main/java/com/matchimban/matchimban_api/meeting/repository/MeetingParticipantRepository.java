package com.matchimban.matchimban_api.meeting.repository;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.projection.MeetingParticipantProfileRow;
import com.matchimban.matchimban_api.meeting.repository.projection.MyMeetingRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    @Query("""
        select new com.matchimban.matchimban_api.meeting.repository.projection.MyMeetingRow(
            mp.id,
            m.id,
            m.title,
            m.scheduledAt,
            (select count(mp2.id)
               from MeetingParticipant mp2
              where mp2.meeting = m
                and mp2.status = :activeStatus
            ),
            m.targetHeadcount,
            (select v.status
               from Vote v
              where v.meeting = m
                and v.round = (select max(v2.round) from Vote v2 where v2.meeting = m)
            ),
            m.isQuickMeeting
        )
        from MeetingParticipant mp
        join mp.meeting m
        where mp.member.id = :memberId
          and mp.status = :activeStatus
          and m.isDeleted = false
          and (:cursor is null or mp.id < :cursor)
        order by mp.id desc
    """)
    List<MyMeetingRow> findMyMeetingRows(
            @Param("memberId") Long memberId,
            @Param("cursor") Long cursor,
            @Param("activeStatus") MeetingParticipant.Status activeStatus,
            Pageable pageable
    );

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
        select
            mp.member.id as memberId,
            mp.member.nickname as nickname,
            mp.member.profileImageUrl as profileImageUrl
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.status = com.matchimban.matchimban_api.meeting.entity.MeetingParticipant.Status.ACTIVE
        order by mp.createdAt asc, mp.id asc
    """)
    List<MeetingParticipantProfileRow> findActiveParticipantProfiles(@Param("meetingId") Long meetingId);


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
        select mp
        from MeetingParticipant mp
        join fetch mp.member
        where mp.meeting.id = :meetingId
          and mp.member.id = :memberId
          and mp.status = :status
    """)
    Optional<MeetingParticipant> findByMeetingIdAndMemberIdAndStatusFetchMember(
            @Param("meetingId") Long meetingId,
            @Param("memberId") Long memberId,
            @Param("status") MeetingParticipant.Status status
    );

    @Query("""
        select mp.lastReadId
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.status = com.matchimban.matchimban_api.meeting.entity.MeetingParticipant.Status.ACTIVE
        order by mp.id asc
    """)
    List<Long> findActiveLastReadIds(@Param("meetingId") Long meetingId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update MeetingParticipant mp
           set mp.lastReadId = :lastReadId
         where mp.meeting.id = :meetingId
           and mp.member.id = :memberId
           and mp.status = :status
           and :lastReadId is not null
           and (mp.lastReadId is null or mp.lastReadId < :lastReadId)
    """)
    int advanceLastReadIdIfGreater(
            @Param("meetingId") Long meetingId,
            @Param("memberId") Long memberId,
            @Param("status") MeetingParticipant.Status status,
            @Param("lastReadId") Long lastReadId
    );

    @Query("""
        select mp.member.id
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.status = com.matchimban.matchimban_api.meeting.entity.MeetingParticipant.Status.ACTIVE
        order by mp.createdAt asc, mp.id asc
    """)
    List<Long> findActiveMemberIds(@Param("meetingId") Long meetingId);

    @EntityGraph(attributePaths = {"meeting", "member"})
    @Query("""
        select mp
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.member.id = :memberId
          and mp.status = :status
          and mp.meeting.isDeleted = false
    """)
    Optional<MeetingParticipant> findByMeetingIdAndMemberIdAndStatusWithGraph(
            @Param("meetingId") Long meetingId,
            @Param("memberId") Long memberId,
            @Param("status") MeetingParticipant.Status status
    );

    @Query("""
        select mp
        from MeetingParticipant mp
        where mp.meeting.id = :meetingId
          and mp.status = com.matchimban.matchimban_api.meeting.entity.MeetingParticipant.Status.ACTIVE
          and mp.meeting.isDeleted = false
        order by mp.createdAt asc, mp.id asc
    """)
    List<MeetingParticipant> findActiveParticipants(@Param("meetingId") Long meetingId);
}
