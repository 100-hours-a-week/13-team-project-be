package com.matchimban.matchimban_api.global.config;

import com.matchimban.matchimban_api.chat.auth.ChatStompAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

	private final ChatStompAuthorizationInterceptor chatStompAuthorizationInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/api/v2/ws")
			.setAllowedOriginPatterns(
				"https://moyeobab.com",
				"https://www.moyeobab.com",
				"https://dev.moyeobab.com",
				"http://localhost:3000",
				"http://localhost:5173"
			);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.setApplicationDestinationPrefixes("/api/v2/app");
		registry.enableSimpleBroker("/api/v2/topic", "/queue");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(chatStompAuthorizationInterceptor);
	}
}
