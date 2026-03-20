package com.matchimban.matchimban_api.chat.service;

import com.matchimban.matchimban_api.chat.entity.ChatMessage;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import com.matchimban.matchimban_api.chat.repository.ChatMessageRepository;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessagePgBridge {

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final ChatMessageRepository chatMessageRepository;

	@Transactional
	public void updateLastChatId(Long meetingId, String messageId) {
		meetingRepository.updateLastChatIdIfGreater(meetingId, messageId);
	}

	@Transactional
	public int advanceLastReadId(Long meetingId, Long memberId, String messageId) {
		return meetingParticipantRepository.advanceLastReadIdIfGreater(
			meetingId, memberId, MeetingParticipant.Status.ACTIVE, messageId
		);
	}

	@Transactional
	public void dualWriteMessage(MeetingParticipant participant, ChatMessageType type,
		String content, String clientMessageId) {
		try {
			ChatMessage pgMessage = ChatMessage.builder()
				.meeting(participant.getMeeting())
				.participant(participant)
				.type(type)
				.clientMessageId(clientMessageId)
				.message(content)
				.build();
			chatMessageRepository.save(pgMessage);
		} catch (Exception ex) {
			log.warn("Dual-write to PG failed (MongoDB is primary, PG write is best-effort)", ex);
		}
	}
}
