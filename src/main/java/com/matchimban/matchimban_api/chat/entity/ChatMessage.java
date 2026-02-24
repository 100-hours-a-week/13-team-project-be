package com.matchimban.matchimban_api.chat.entity;

import com.matchimban.matchimban_api.meeting.entity.Meeting;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "chat_messages")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_messages_seq_gen")
	@SequenceGenerator(
		name = "chat_messages_seq_gen",
		sequenceName = "chat_messages_seq",
		allocationSize = 1
	)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "meeting_id", nullable = false)
	private Meeting meeting;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "participant_id", nullable = false)
	private MeetingParticipant participant;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private ChatMessageType type;

	@Column(name = "client_message_id", length = 64)
	private String clientMessageId;

	@Column(name = "message", nullable = false, columnDefinition = "TEXT")
	private String message;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@CreatedDate
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
