package com.matchimban.matchimban_api.vote.repository;

import com.matchimban.matchimban_api.vote.dto.response.FinalSelectionResponse;
import com.matchimban.matchimban_api.vote.entity.MeetingFinalSelection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingFinalSelectionRepository extends JpaRepository<MeetingFinalSelection, Long> {

    boolean existsByMeetingId(Long meetingId);

    Optional<MeetingFinalSelection> findByMeetingId(Long meetingId);

    @Query("""
        select new com.matchimban.matchimban_api.vote.dto.response.FinalSelectionResponse(
            c.id,
            r.id,
            (
                select rv.id
                from Review rv
                where rv.meeting.id = fs.meeting.id
                  and rv.member.id = :memberId
                  and rv.isDeleted = false
            ),
            r.name,
            r.imageUrl1,
            r.imageUrl2,
            r.imageUrl3,
            fc.categoryName,
            c.rating,
            c.distanceM,
            r.roadAddress,
            r.jibunAddress
        )
        from MeetingFinalSelection fs
        join fs.finalCandidate c
        join c.restaurant r
        join r.foodCategory fc
        where fs.meeting.id = :meetingId
    """)
    Optional<FinalSelectionResponse> findFinalSelectionResponseByMeetingIdAndMemberId(
            @Param("meetingId") Long meetingId,
            @Param("memberId") Long memberId
    );
}
