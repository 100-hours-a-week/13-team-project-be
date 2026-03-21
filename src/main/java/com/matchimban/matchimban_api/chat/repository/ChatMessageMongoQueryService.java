package com.matchimban.matchimban_api.chat.repository;

import com.matchimban.matchimban_api.chat.document.ChatMessageDocument;
import com.matchimban.matchimban_api.chat.entity.ChatMessageType;
import com.matchimban.matchimban_api.chat.repository.projection.ChatMessageRow;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageMongoQueryService {

	private final MongoTemplate mongoTemplate;

	public List<ChatMessageRow> findPageRows(Long meetingId, String cursor, int limit) {
		Criteria criteria = Criteria.where("meetingId").is(meetingId)
			.and("isDeleted").is(false);

		if (cursor != null) {
			criteria = criteria.and("_id").lt(new ObjectId(cursor));
		}

		Query query = new Query(criteria)
			.with(Sort.by(Sort.Direction.DESC, "_id"))
			.limit(limit);

		List<ChatMessageDocument> docs = mongoTemplate.find(query, ChatMessageDocument.class);

		return docs.stream()
			.map(this::toRow)
			.toList();
	}

	public List<String> findRecentMessageIds(Long meetingId, int limit) {
		Criteria criteria = Criteria.where("meetingId").is(meetingId)
			.and("isDeleted").is(false);

		Query query = new Query(criteria)
			.with(Sort.by(Sort.Direction.DESC, "_id"))
			.limit(limit);
		query.fields().include("_id");

		List<ChatMessageDocument> docs = mongoTemplate.find(query, ChatMessageDocument.class);

		return docs.stream()
			.map(ChatMessageDocument::getId)
			.toList();
	}

	public long countUnreadForMeetingBadge(
		Long meetingId, Long memberId, String lastReadId, ChatMessageType excludedType
	) {
		Criteria criteria = Criteria.where("meetingId").is(meetingId)
			.and("isDeleted").is(false)
			.and("type").ne(excludedType)
			.and("senderId").ne(memberId);

		if (lastReadId != null && ObjectId.isValid(lastReadId)) {
			criteria = criteria.and("_id").gt(new ObjectId(lastReadId));
		}

		Query query = new Query(criteria);
		return mongoTemplate.count(query, ChatMessageDocument.class);
	}

	private ChatMessageRow toRow(ChatMessageDocument doc) {
		return new ChatMessageRow(
			doc.getId(),
			doc.getType(),
			doc.getContent(),
			doc.getCreatedAt(),
			doc.getSenderId(),
			doc.getSenderName(),
			doc.getSenderProfileImageUrl()
		);
	}
}
