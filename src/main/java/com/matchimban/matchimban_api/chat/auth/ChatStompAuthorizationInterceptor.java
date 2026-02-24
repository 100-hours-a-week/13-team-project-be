package com.matchimban.matchimban_api.chat.auth;

import com.matchimban.matchimban_api.auth.jwt.MemberPrincipal;
import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;
import com.matchimban.matchimban_api.meeting.repository.MeetingParticipantRepository;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatStompAuthorizationInterceptor implements ChannelInterceptor {

	private static final Pattern MEETING_DESTINATION_PATTERN = Pattern.compile(
		"^/api/v2/(?:app|topic)/meetings/(\\d+)/(?:messages|unread-counts)$"
	);

	private final MeetingParticipantRepository meetingParticipantRepository;
	private final StompPrincipalExtractor stompPrincipalExtractor;

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		StompCommand command = accessor.getCommand();
		if (command == null) {
			return message;
		}

		if (command == StompCommand.CONNECT) {
			validateAuthenticated(accessor.getUser());
			return message;
		}

		if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
			return message;
		}

		MemberPrincipal principal = validateAuthenticated(accessor.getUser());
		String destination = accessor.getDestination();
		if (destination == null || destination.isBlank()) {
			return message;
		}
		if ("/api/v2/app/heartbeat".equals(destination)) {
			return message;
		}

		Matcher matcher = MEETING_DESTINATION_PATTERN.matcher(destination);
		if (!matcher.matches()) {
			return message;
		}

		Long meetingId = Long.parseLong(matcher.group(1));
		boolean allowed = meetingParticipantRepository.existsByMeetingIdAndMemberIdAndStatus(
			meetingId,
			principal.memberId(),
			MeetingParticipant.Status.ACTIVE
		);
		if (!allowed) {
			throw new AccessDeniedException("forbidden");
		}
		return message;
	}

	private MemberPrincipal validateAuthenticated(Principal principal) {
		MemberPrincipal memberPrincipal = stompPrincipalExtractor.extract(principal);
		if (memberPrincipal == null) {
			throw new AuthenticationCredentialsNotFoundException("unauthorized");
		}
		return memberPrincipal;
	}
}
