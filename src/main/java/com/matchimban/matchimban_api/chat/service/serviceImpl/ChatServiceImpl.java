package com.matchimban.matchimban_api.chat.service.serviceImpl;

import com.matchimban.matchimban_api.chat.cache.ChatMessageCacheService;
import com.matchimban.matchimban_api.chat.document.ChatMessageDocument;
import com.matchimban.matchimban_api.chat.dto.http.ChatReadPointerUpdatedData;
import com.matchimban.matchimban_api.chat.dto.ChatSenderDto;
import com.matchimban.matchimban_api.chat.dto.http.ChatMessageItemDto;
import com.matchimban.matchimban_api.chat.dto.http.ChatMessagePageDto;
import com.matchimban.matchimban_api.chat.dto.http.ChatMessagesLoadedData;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageCreatedData;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageCreatedEvent;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageSendAckData;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageSendAckEvent;
import com.matchimban.matchimban_api.chat.dto.ws.ChatSendMessageRequest;
import com.matchimban.matchimban_api.chat.dto.ws.ChatUnreadCountItem;
import com.matchimban.matchimban_api.chat.dto.ws.ChatUnreadCountsBasis;
import com.matchimban.matchimban_api.chat.dto.ws.ChatUnreadCountsUpdatedData;
import com.matchimban.matchimban_api.chat.dto.ws.ChatUnreadCountsUpdatedEvent;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import com.matchimban.matchimban_api.chat.error.ChatErrorCode;
import com.matchimban.matchimban_api.chat.event.ChatMessageCreatedInternalEvent;
import com.matchimban.matchimban_api.chat.event.ChatUnreadCountsRefreshInternalEvent;
import com.matchimban.matchimban_api.chat.metrics.ChatMetricsRecorder;
import com.matchimban.matchimban_api.chat.redis.ChatRedisPublisher;
import com.matchimban.matchimban_api.chat.repository.ChatMessageMongoQueryService;
import com.matchimban.matchimban_api.chat.repository.ChatMessageMongoRepository;
import com.matchimban.matchimban_api.chat.repository.projection.ChatMessageRow;
import com.matchimban.matchimban_api.chat.service.ChatMessagePgBridge;
import com.matchimban.matchimban_api.chat.service.ChatService;
import com.matchimban.matchimban_api.chat.service.ChatSystemMessageService;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService, ChatSystemMessageService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final ChatMessageMongoRepository chatMessageMongoRepository;
	private final ChatMessageMongoQueryService chatMessageMongoQueryService;
	private final MeetingParticipantRepository meetingParticipantRepository;
	private final ChatMessagePgBridge chatMessagePgBridge;
	private final ChatMessageCacheService chatMessageCacheService;
	private final ChatRedisPublisher chatRedisPublisher;
	private final StringRedisTemplate stringRedisTemplate;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final ChatMetricsRecorder chatMetricsRecorder;

	@Value("${chat.unread.window-size:50}")
	private int unreadWindowSize;

	@Override
	public ChatMessagesLoadedData getMessages(Long memberId, Long meetingId, String cursor, int size) {
		assertActiveParticipant(memberId, meetingId);

		List<ChatMessageRow> rows = loadMessageRows(meetingId, cursor, size);

		boolean hasNext = rows.size() > size;
		List<ChatMessageRow> pageRows = hasNext ? rows.subList(0, size) : rows;
		String nextCursor = hasNext && !pageRows.isEmpty()
			? pageRows.get(pageRows.size() - 1).messageId()
			: null;

		Map<String, Integer> unreadCountMap = buildUnreadCountMap(meetingId, pageRows);

		List<ChatMessageItemDto> items = pageRows.stream()
			.map(row -> toMessageItemDto(row, unreadCountMap.get(row.messageId())))
			.toList();

		return new ChatMessagesLoadedData(
			items,
			new ChatMessagePageDto(size, nextCursor, hasNext)
		);
	}

	private List<ChatMessageRow> loadMessageRows(Long meetingId, String cursor, int size) {
		int fetchSize = size + 1;
		if (cursor != null || !chatMessageCacheService.isEnabled()) {
			return chatMessageMongoQueryService.findPageRows(meetingId, cursor, fetchSize);
		}

		boolean cacheEligible = chatMessageCacheService.recordTrafficAndIsCacheEligible(meetingId);
		if (!cacheEligible) {
			return chatMessageMongoQueryService.findPageRows(meetingId, cursor, fetchSize);
		}

		Optional<List<ChatMessageRow>> cachedRows = chatMessageCacheService.getRecentMessages(meetingId, fetchSize);
		if (cachedRows.isPresent()) {
			return cachedRows.get();
		}

		int cacheWindowFetchSize = Math.max(fetchSize, chatMessageCacheService.recentWindowSize() + 1);
		List<ChatMessageRow> dbRows = chatMessageMongoQueryService.findPageRows(meetingId, null, cacheWindowFetchSize);
		if (!dbRows.isEmpty()) {
			chatMessageCacheService.replaceRecentMessages(meetingId, dbRows);
		}
		return dbRows;
	}

	@Override
	public ChatMessageSendAckEvent sendMessage(Long memberId, Long meetingId, ChatSendMessageRequest request) {
		chatMetricsRecorder.recordSendAttempt();
		MeetingParticipant participant = meetingParticipantRepository
			.findByMeetingIdAndMemberIdAndStatusFetchMember(
				meetingId,
				memberId,
				MeetingParticipant.Status.ACTIVE
			)
			.orElseThrow(() -> new ApiException(ChatErrorCode.FORBIDDEN));

		validateSendRequest(request);

		String clientMessageId = normalizeClientMessageId(request.clientMessageId());
		if (clientMessageId != null) {
			Optional<ChatMessageDocument> existing = chatMessageMongoRepository
				.findByMeetingIdAndParticipantIdAndClientMessageId(
					meetingId,
					participant.getId(),
					clientMessageId
				);
			if (existing.isPresent()) {
				chatMetricsRecorder.recordSendAccepted(true);
				return toAcceptedAck(meetingId, clientMessageId, existing.get());
			}
		}

		ChatMessageDocument saved;
		try {
			saved = persistMessage(
				participant,
				request.type(),
				request.content(),
				clientMessageId
			);
		} catch (DuplicateKeyException ex) {
			if (clientMessageId != null) {
				ChatMessageDocument existing = chatMessageMongoRepository
					.findByMeetingIdAndParticipantIdAndClientMessageId(
						meetingId,
						participant.getId(),
						clientMessageId
					)
					.orElseThrow(() -> ex);
				chatMetricsRecorder.recordSendAccepted(true);
				return toAcceptedAck(meetingId, clientMessageId, existing);
			}
			throw ex;
		}

		publishMessageCreated(saved, participant);
		chatMessagePgBridge.advanceLastReadId(meetingId, memberId, saved.getId());
		scheduleUnreadCountsRefresh(meetingId);
		chatMetricsRecorder.recordSendAccepted(false);
		return toAcceptedAck(meetingId, clientMessageId, saved);
	}

	@Override
	public ChatReadPointerUpdatedData updateReadPointer(Long memberId, Long meetingId, String lastReadMessageId) {
		assertActiveParticipant(memberId, meetingId);
		boolean exists = chatMessageMongoRepository.existsByIdAndMeetingIdAndIsDeletedFalse(
			lastReadMessageId, meetingId
		);
		if (!exists) {
			throw new ApiException(ChatErrorCode.INVALID_READ_POINTER);
		}

		int updatedRows = chatMessagePgBridge.advanceLastReadId(meetingId, memberId, lastReadMessageId);
		if (updatedRows > 0) {
			scheduleUnreadCountsRefresh(meetingId);
		}
		chatMetricsRecorder.recordReadPointerUpdate(updatedRows > 0);

		return new ChatReadPointerUpdatedData(meetingId, lastReadMessageId, updatedRows > 0);
	}

	@Override
	public void publishUnreadCountsWindow(Long meetingId) {
		int windowSize = Math.max(1, unreadWindowSize);
		List<String> recentMessageIdsDesc = chatMessageMongoQueryService.findRecentMessageIds(
			meetingId, windowSize
		);
		if (recentMessageIdsDesc.isEmpty()) {
			return;
		}

		List<String> recentMessageIds = new ArrayList<>(recentMessageIdsDesc);
		Collections.reverse(recentMessageIds);

		Map<String, Integer> unreadCountMap = computeUnreadCounts(meetingId, recentMessageIds);
		List<ChatUnreadCountItem> items = recentMessageIds.stream()
			.map(messageId -> new ChatUnreadCountItem(messageId, unreadCountMap.getOrDefault(messageId, 0)))
			.toList();

		String fromMessageId = recentMessageIds.get(0);
		String toMessageId = recentMessageIds.get(recentMessageIds.size() - 1);
		long serverVersion = nextUnreadServerVersion(meetingId);

		ChatUnreadCountsUpdatedData data = new ChatUnreadCountsUpdatedData(
			meetingId,
			new ChatUnreadCountsBasis(windowSize, fromMessageId, toMessageId),
			items,
			serverVersion,
			OffsetDateTime.now(KST)
		);
		chatRedisPublisher.publishUnreadCountsUpdated(ChatUnreadCountsUpdatedEvent.of(data));
	}

	@Override
	public long countUnreadForMeetingBadge(Long meetingId, Long memberId) {
		MeetingParticipant participant = meetingParticipantRepository
			.findByMeetingIdAndMemberIdAndStatusFetchMember(
				meetingId,
				memberId,
				MeetingParticipant.Status.ACTIVE
			)
			.orElse(null);

		if (participant == null) {
			return 0;
		}

		String lastReadId = participant.getLastReadId();
		return chatMessageMongoQueryService.countUnreadForMeetingBadge(
			meetingId, memberId, lastReadId, ChatMessageType.SYSTEM
		);
	}

	private void assertActiveParticipant(Long memberId, Long meetingId) {
		boolean isActiveParticipant = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
			meetingId,
			memberId,
			MeetingParticipant.Status.ACTIVE
		);
		if (!isActiveParticipant) {
			throw new ApiException(ChatErrorCode.FORBIDDEN);
		}
	}

	private void validateSendRequest(ChatSendMessageRequest request) {
		if (request.type() == ChatMessageType.SYSTEM) {
			throw new ApiException(ChatErrorCode.INVALID_MESSAGE_TYPE);
		}
		if (request.content() == null || request.content().isBlank()) {
			throw new ApiException(ChatErrorCode.INVALID_MESSAGE_CONTENT);
		}
	}

	private String normalizeClientMessageId(String clientMessageId) {
		if (clientMessageId == null) {
			return null;
		}
		String normalized = clientMessageId.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private ChatMessageItemDto toMessageItemDto(ChatMessageRow row, Integer unreadCount) {
		ChatSenderDto sender = row.type() == ChatMessageType.SYSTEM
			? null
			: new ChatSenderDto(row.senderId(), row.senderName(), row.senderProfileImageUrl());

		return new ChatMessageItemDto(
			row.messageId(),
			unreadCount,
			row.type(),
			row.content(),
			sender,
			toKstOffsetDateTime(row.createdAt())
		);
	}

	private Map<String, Integer> buildUnreadCountMap(Long meetingId, List<ChatMessageRow> pageRows) {
		List<String> messageIds = pageRows.stream()
			.map(ChatMessageRow::messageId)
			.toList();
		return computeUnreadCounts(meetingId, messageIds);
	}

	private Map<String, Integer> computeUnreadCounts(Long meetingId, List<String> messageIds) {
		if (messageIds.isEmpty()) {
			return Map.of();
		}

		List<String> activeLastReadIds = meetingParticipantRepository.findActiveLastReadIds(meetingId);
		Map<String, Integer> result = new HashMap<>();
		for (String messageId : messageIds) {
			int unreadCount = 0;
			for (String lastReadId : activeLastReadIds) {
				if (lastReadId == null || lastReadId.compareTo(messageId) < 0) {
					unreadCount++;
				}
			}
			result.put(messageId, unreadCount);
		}
		return result;
	}

	private OffsetDateTime toKstOffsetDateTime(Instant instant) {
		return instant == null ? null : instant.atZone(KST).toOffsetDateTime();
	}

	@Override
	public void publishSystemMessage(MeetingParticipant participant, String content) {
		if (content == null || content.isBlank()) {
			return;
		}
		ChatMessageDocument saved = persistMessage(participant, ChatMessageType.SYSTEM, content, null);
		chatMessagePgBridge.advanceLastReadId(
			participant.getMeeting().getId(), participant.getMember().getId(), saved.getId()
		);
		publishMessageCreated(saved, participant);
		scheduleUnreadCountsRefresh(participant.getMeeting().getId());
	}

	private ChatMessageDocument persistMessage(
		MeetingParticipant participant,
		ChatMessageType type,
		String content,
		String clientMessageId
	) {
		Long senderId = participant.getMember().getId();
		String senderName = participant.getMember().getNickname();
		String senderProfileImageUrl = participant.getMember().getProfileImageUrl();

		ChatMessageDocument doc = ChatMessageDocument.builder()
			.meetingId(participant.getMeeting().getId())
			.participantId(participant.getId())
			.senderId(senderId)
			.senderName(senderName)
			.senderProfileImageUrl(senderProfileImageUrl)
			.type(type)
			.clientMessageId(clientMessageId)
			.content(content)
			.build();

		ChatMessageDocument saved = chatMessageMongoRepository.save(doc);
		chatMessagePgBridge.updateLastChatId(participant.getMeeting().getId(), saved.getId());
		chatMessagePgBridge.dualWriteMessage(participant, type, content, clientMessageId);
		chatMetricsRecorder.recordMessagePersisted(type);
		return saved;
	}


	private ChatMessageSendAckEvent toAcceptedAck(Long meetingId, String clientMessageId, ChatMessageDocument message) {
		ChatMessageSendAckData ackData = new ChatMessageSendAckData(
			meetingId,
			clientMessageId,
			message.getId(),
			"ACCEPTED",
			toKstOffsetDateTime(message.getCreatedAt())
		);
		return ChatMessageSendAckEvent.accepted(ackData);
	}

	private void scheduleUnreadCountsRefresh(Long meetingId) {
		applicationEventPublisher.publishEvent(new ChatUnreadCountsRefreshInternalEvent(meetingId));
	}

	private long nextUnreadServerVersion(Long meetingId) {
		Long version = stringRedisTemplate.opsForValue().increment("chat:meeting:unread-version:" + meetingId);
		return version == null ? 0L : version;
	}

	private void publishMessageCreated(ChatMessageDocument saved, MeetingParticipant participant) {
		ChatSenderDto sender = saved.getType() == ChatMessageType.SYSTEM
			? null
			: new ChatSenderDto(
				participant.getMember().getId(),
				participant.getMember().getNickname(),
				participant.getMember().getProfileImageUrl()
			);

		ChatMessageCreatedData createdData = new ChatMessageCreatedData(
			saved.getMeetingId(),
			saved.getId(),
			saved.getType(),
			saved.getContent(),
			sender,
			toKstOffsetDateTime(saved.getCreatedAt())
		);
		applicationEventPublisher.publishEvent(
			new ChatMessageCreatedInternalEvent(ChatMessageCreatedEvent.of(createdData))
		);
	}
}
