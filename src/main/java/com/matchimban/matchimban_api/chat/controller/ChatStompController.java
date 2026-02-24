package com.matchimban.matchimban_api.chat.controller;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.chat.auth.StompPrincipalExtractor;
import com.matchimban.matchimban_api.chat.dto.ws.ChatHeartbeatAckMessage;
import com.matchimban.matchimban_api.chat.dto.ws.ChatHeartbeatRequest;
import com.matchimban.matchimban_api.chat.dto.ws.ChatMessageSendAckEvent;
import com.matchimban.matchimban_api.chat.dto.ws.ChatSendMessageRequest;
import com.matchimban.matchimban_api.chat.service.ChatService;
import com.matchimban.matchimban_api.global.error.api.ApiException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final ChatService chatService;
	private final StompPrincipalExtractor stompPrincipalExtractor;

	@MessageMapping("/meetings/{meetingId}/messages")
	@SendToUser(value = "/queue/messages/ack", broadcast = false)
	public ChatMessageSendAckEvent sendMessage(
		@DestinationVariable Long meetingId,
		@Valid @Payload ChatSendMessageRequest request,
		Principal principal
	) {
		MemberPrincipal memberPrincipal = requirePrincipal(principal);
		return chatService.sendMessage(memberPrincipal.memberId(), meetingId, request);
	}

	@MessageMapping("/heartbeat")
	@SendToUser(value = "/queue/heartbeat", broadcast = false)
	public ChatHeartbeatAckMessage heartbeat(
		@Payload ChatHeartbeatRequest request,
		Principal principal
	) {
		requirePrincipal(principal);
		return ChatHeartbeatAckMessage.now(OffsetDateTime.now(KST));
	}

	@MessageExceptionHandler(ApiException.class)
	@SendToUser(value = "/queue/errors", broadcast = false)
	public Map<String, String> handleApiException(ApiException ex) {
		return Map.of("message", ex.getErrorCode().getMessage());
	}

	@MessageExceptionHandler(Exception.class)
	@SendToUser(value = "/queue/errors", broadcast = false)
	public Map<String, String> handleException() {
		return Map.of("message", "internal_server_error");
	}

	private MemberPrincipal requirePrincipal(Principal principal) {
		MemberPrincipal memberPrincipal = stompPrincipalExtractor.extract(principal);
		if (memberPrincipal == null) {
			throw new AuthenticationCredentialsNotFoundException("unauthorized");
		}
		return memberPrincipal;
	}
}
