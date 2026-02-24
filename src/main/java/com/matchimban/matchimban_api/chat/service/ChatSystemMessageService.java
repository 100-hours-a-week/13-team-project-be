package com.matchimban.matchimban_api.chat.service;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;

public interface ChatSystemMessageService {

	void publishSystemMessage(MeetingParticipant participant, String content);
}
