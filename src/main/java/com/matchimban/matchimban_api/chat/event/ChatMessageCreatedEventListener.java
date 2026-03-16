package com.matchimban.matchimban_api.chat.event;

import com.matchimban.matchimban_api.chat.cache.ChatMessageCacheService;
import com.matchimban.matchimban_api.chat.dto.ChatSenderDto;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageCreatedData;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import com.matchimban.matchimban_api.notification.entity.NotificationType;
import com.matchimban.matchimban_api.notification.service.NotificationCommandService;
import com.matchimban.matchimban_api.chat.redis.ChatRedisPublisher;
import com.matchimban.matchimban_api.chat.repository.projection.ChatMessageRow;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageCreatedEventListener {

	private final ChatMessageCacheService chatMessageCacheService;
	private final ChatRedisPublisher chatRedisPublisher;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final NotificationCommandService notificationCommandService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onChatMessageCreated(ChatMessageCreatedInternalEvent event) {
		try {
			ChatMessageCreatedData data = event.payload().data();
			ChatMessageRow row = toRow(data);
			chatMessageCacheService.appendRecentMessage(data.meetingId(), row);
		} catch (Exception ex) {
			log.warn("Failed to update recent-message cache before publish", ex);
		}

		try {
			chatRedisPublisher.publishMessageCreated(event.payload());
		} catch (Exception ex) {
			log.error("Failed to publish chat message after commit", ex);
		}

		try {
			publishChatMessageNotification(event.payload().data());
		} catch (Exception ex) {
			log.error("Failed to create chat notification", ex);
		}
	}

	private ChatMessageRow toRow(ChatMessageCreatedData data) {
		ChatSenderDto sender = data.sender();
		return new ChatMessageRow(
			data.messageId(),
			data.type(),
			data.content(),
			toInstant(data.createdAt()),
			sender == null ? null : sender.userId(),
			sender == null ? null : sender.name(),
			sender == null ? null : sender.profileImageUrl()
		);
	}

	private Instant toInstant(OffsetDateTime createdAt) {
		return createdAt == null ? null : createdAt.toInstant();
	}

	private void publishChatMessageNotification(ChatMessageCreatedData data) {
		if (data == null || data.type() == ChatMessageType.SYSTEM) {
			return;
		}
		if (data.sender() == null || data.sender().userId() == null) {
			return;
		}

		Long senderMemberId = data.sender().userId();
		List<Long> recipients = meetingParticipantRepository.findActiveMemberIds(data.meetingId()).stream()
			.filter(memberId -> !memberId.equals(senderMemberId))
			.toList();

		if (recipients.isEmpty()) {
			return;
		}

		String senderName = safeSenderName(data.sender().name());
		String content = data.content() == null ? "" : data.content();

		notificationCommandService.createNotifications(
			NotificationType.CHAT_MESSAGE,
			"새 채팅 메시지",
			senderName + ": " + truncate(content, 120),
			"MEETING",
			data.meetingId(),
			data.messageId(),
			"/meetings/" + data.meetingId() + "/chat",
			"CHAT_MESSAGE:" + data.meetingId() + ":" + data.messageId(),
			null,
			recipients
		);
	}

	private String safeSenderName(String name) {
		if (name == null || name.isBlank()) {
			return "참여자";
		}
		return name;
	}

	private String truncate(String value, int maxLength) {
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
