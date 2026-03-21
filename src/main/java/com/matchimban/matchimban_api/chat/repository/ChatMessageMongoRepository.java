package com.matchimban.matchimban_api.chat.repository;

import com.matchimban.matchimban_api.chat.document.ChatMessageDocument;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {

	Optional<ChatMessageDocument> findByMeetingIdAndParticipantIdAndClientMessageId(
		Long meetingId, Long participantId, String clientMessageId
	);

	boolean existsByIdAndMeetingIdAndIsDeletedFalse(String id, Long meetingId);

	long countByMeetingIdAndIsDeletedFalseAndTypeNotAndSenderIdNotAndIdGreaterThan(
		Long meetingId, ChatMessageType excludedType, Long excludedSenderId, String lastReadId
	);
}
