package com.matchimban.matchimban_api.chat.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Predicate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

@Configuration
@ConditionalOnBean(SimpUserRegistry.class)
public class ChatWebSocketMetricsConfig {

	private final SimpUserRegistry simpUserRegistry;

	public ChatWebSocketMetricsConfig(
		MeterRegistry meterRegistry,
		SimpUserRegistry simpUserRegistry
	) {
		this.simpUserRegistry = simpUserRegistry;

		Gauge.builder("chat.websocket.connected.users", this, metrics -> metrics.countConnectedUsers())
			.description("Current number of connected STOMP users")
			.register(meterRegistry);
		Gauge.builder("chat.websocket.connected.sessions", this, metrics -> metrics.countConnectedSessions())
			.description("Current number of connected STOMP sessions")
			.register(meterRegistry);
		Gauge.builder("chat.websocket.subscriptions.total", this, metrics -> metrics.countTotalSubscriptions())
			.description("Current number of STOMP subscriptions")
			.register(meterRegistry);
		Gauge.builder("chat.websocket.subscriptions.messages_topic", this, metrics -> metrics.countMessagesTopicSubscriptions())
			.description("Subscriptions to /api/v2/topic/meetings/{id}/messages")
			.register(meterRegistry);
		Gauge.builder("chat.websocket.subscriptions.unread_topic", this, metrics -> metrics.countUnreadTopicSubscriptions())
			.description("Subscriptions to /api/v2/topic/meetings/{id}/unread-counts")
			.register(meterRegistry);
		Gauge.builder("chat.websocket.subscriptions.ack_queue", this, metrics -> metrics.countAckQueueSubscriptions())
			.description("Subscriptions to /user/queue/messages/ack (resolved queue destination)")
			.register(meterRegistry);
		Gauge.builder("chat.websocket.subscriptions.other", this, metrics -> metrics.countOtherSubscriptions())
			.description("Subscriptions that do not match known chat destinations")
			.register(meterRegistry);
	}

	private double countConnectedUsers() {
		return simpUserRegistry.getUserCount();
	}

	private double countConnectedSessions() {
		int sessionCount = 0;
		for (SimpUser user : simpUserRegistry.getUsers()) {
			sessionCount += user.getSessions().size();
		}
		return sessionCount;
	}

	private double countTotalSubscriptions() {
		return countSubscriptions(destination -> true);
	}

	private double countMessagesTopicSubscriptions() {
		return countSubscriptions(this::isMessagesTopicDestination);
	}

	private double countUnreadTopicSubscriptions() {
		return countSubscriptions(this::isUnreadTopicDestination);
	}

	private double countAckQueueSubscriptions() {
		return countSubscriptions(this::isAckQueueDestination);
	}

	private double countOtherSubscriptions() {
		double total = countTotalSubscriptions();
		double known = countMessagesTopicSubscriptions() + countUnreadTopicSubscriptions() + countAckQueueSubscriptions();
		return Math.max(0, total - known);
	}

	private int countSubscriptions(Predicate<String> destinationPredicate) {
		int count = 0;
		for (SimpUser user : simpUserRegistry.getUsers()) {
			for (SimpSession session : user.getSessions()) {
				for (SimpSubscription subscription : session.getSubscriptions()) {
					if (destinationPredicate.test(subscription.getDestination())) {
						count += 1;
					}
				}
			}
		}
		return count;
	}

	private boolean isMessagesTopicDestination(String destination) {
		return destination != null
			&& destination.startsWith("/api/v2/topic/meetings/")
			&& destination.endsWith("/messages");
	}

	private boolean isUnreadTopicDestination(String destination) {
		return destination != null
			&& destination.startsWith("/api/v2/topic/meetings/")
			&& destination.endsWith("/unread-counts");
	}

	private boolean isAckQueueDestination(String destination) {
		return destination != null && destination.contains("/queue/messages/ack");
	}
}
