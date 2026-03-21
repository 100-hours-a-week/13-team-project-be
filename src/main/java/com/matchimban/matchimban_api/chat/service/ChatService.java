package com.matchimban.matchimban_api.chat.service;

import com.matchimban.matchimban_api.chat.dto.http.ChatMessagesLoadedData;
import com.matchimban.matchimban_api.chat.dto.http.ChatReadPointerUpdatedData;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageSendAckEvent;
import com.matchimban.matchimban_api.chat.dto.ws.ChatSendMessageRequest;

public interface ChatService {

	ChatMessagesLoadedData getMessages(Long memberId, Long meetingId, String cursor, int size);

	ChatMessageSendAckEvent sendMessage(Long memberId, Long meetingId, ChatSendMessageRequest request);

	ChatReadPointerUpdatedData updateReadPointer(Long memberId, Long meetingId, String lastReadMessageId);

	void publishUnreadCountsWindow(Long meetingId);

	long countUnreadForMeetingBadge(Long meetingId, Long memberId);
}
