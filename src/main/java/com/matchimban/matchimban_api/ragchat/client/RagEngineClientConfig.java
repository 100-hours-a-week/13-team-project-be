package com.matchimban.matchimban_api.ragchat.client;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class RagEngineClientConfig {

	@Bean
	public WebClient ragChatWebClient(
		@Value("${rag-chat.base-url:http://localhost:8001}") String baseUrl,
		@Value("${rag-chat.connect-timeout-ms:2000}") int connectTimeoutMillis,
		@Value("${rag-chat.read-timeout-ms:10000}") long readTimeoutMillis
	) {
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, sanitizeConnectTimeout(connectTimeoutMillis))
			.responseTimeout(Duration.ofMillis(sanitizeReadTimeout(readTimeoutMillis)));

		return WebClient.builder()
			.baseUrl(baseUrl)
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.build();
	}

	private int sanitizeConnectTimeout(int timeoutMillis) {
		return Math.max(100, timeoutMillis);
	}

	private long sanitizeReadTimeout(long timeoutMillis) {
		return Math.max(1000L, timeoutMillis);
	}
}
