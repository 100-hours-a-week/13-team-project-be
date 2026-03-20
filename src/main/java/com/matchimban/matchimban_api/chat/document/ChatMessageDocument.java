package com.matchimban.matchimban_api.chat.document;

import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "chat_messages")
@CompoundIndexes({
	@CompoundIndex(name = "idx_meeting_id_desc", def = "{'meetingId': 1, '_id': -1}"),
	@CompoundIndex(
		name = "idx_idempotency",
		def = "{'meetingId': 1, 'participantId': 1, 'clientMessageId': 1}",
		unique = true,
		sparse = true
	)
})
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageDocument {

	@Id
	private String id;

	@Field("meetingId")
	private Long meetingId;

	@Field("participantId")
	private Long participantId;

	@Field("senderId")
	private Long senderId;

	@Field("senderName")
	private String senderName;

	@Field("senderProfileImageUrl")
	private String senderProfileImageUrl;

	@Field("type")
	private ChatMessageType type;

	@Field("clientMessageId")
	private String clientMessageId;

	@Field("content")
	private String content;

	@Builder.Default
	@Field("isDeleted")
	private boolean isDeleted = false;

	@CreatedDate
	@Field("createdAt")
	private Instant createdAt;
}
