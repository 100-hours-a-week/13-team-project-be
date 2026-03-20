package com.matchimban.matchimban_api.chat.service;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.meeting.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessagePgBridge {

	private final MeetingRepository meetingRepository;
	private final MeetingParticipantRepository meetingParticipantRepository;

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
}
