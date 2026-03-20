package com.matchimban.matchimban_api.chat.repository;

import com.matchimban.matchimban_api.chat.entity.ChatMessage;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.chat.repository.projection.ChatMessageRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	@Query("""
		select new com.matchimban.matchimban_api.chat.repository.projection.ChatMessageRow(
			cm.id,
			cm.type,
			cm.message,
			cm.createdAt,
			m.id,
			m.nickname,
			m.profileImageUrl
		)
		from ChatMessage cm
		join cm.participant mp
		join mp.member m
		where cm.meeting.id = :meetingId
		  and cm.isDeleted = false
		  and (:cursor is null or cm.id < :cursor)
		order by cm.id desc
	""")
	List<ChatMessageRow> findPageRows(
		@Param("meetingId") Long meetingId,
		@Param("cursor") Long cursor,
		Pageable pageable
	);

	@Query("""
		select cm.id
		from ChatMessage cm
		where cm.meeting.id = :meetingId
		  and cm.isDeleted = false
		order by cm.id desc
	""")
	List<Long> findRecentMessageIds(
		@Param("meetingId") Long meetingId,
		Pageable pageable
	);

	@Query("""
		select cm
		from ChatMessage cm
		where cm.meeting.id = :meetingId
		  and cm.participant.id = :participantId
		  and cm.clientMessageId = :clientMessageId
	""")
	Optional<ChatMessage> findExistingByIdempotencyKey(
		@Param("meetingId") Long meetingId,
		@Param("participantId") Long participantId,
		@Param("clientMessageId") String clientMessageId
	);

	@Query("""
		select (count(cm) > 0)
		from ChatMessage cm
		where cm.id = :messageId
		  and cm.meeting.id = :meetingId
		  and cm.isDeleted = false
	""")
	boolean existsActiveMessageInMeeting(
		@Param("meetingId") Long meetingId,
		@Param("messageId") Long messageId
	);

	@Query("""
		select count(cm)
		from ChatMessage cm
		where cm.meeting.id = :meetingId
		  and cm.isDeleted = false
		  and cm.type <> :excludedType
		  and cm.participant.member.id <> :memberId
		  and cm.id > coalesce((
			select mp.lastReadId
			from MeetingParticipant mp
			where mp.meeting.id = :meetingId
			  and mp.member.id = :memberId
			  and mp.status = :activeStatus
		  ), 0)
	""")
	long countUnreadForMeetingBadge(
		@Param("meetingId") Long meetingId,
		@Param("memberId") Long memberId,
		@Param("activeStatus") MeetingParticipant.Status activeStatus,
		@Param("excludedType") ChatMessageType excludedType
	);
}
