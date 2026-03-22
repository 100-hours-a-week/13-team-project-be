package com.matchimban.matchimban_api.ragchat.queue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagChatRabbitConfig {

	@Value("${rag-chat.queue.name:rag.chat.jobs}")
	private String queueName;

	@Bean
	public DirectExchange ragChatExchange() {
		return new DirectExchange("rag.chat.exchange");
	}

	@Bean
	public Queue ragChatJobQueue() {
		return QueueBuilder.durable(queueName)
			.deadLetterExchange("rag.chat.dlx")
			.deadLetterRoutingKey("rag.chat.job.dead")
			.build();
	}

	@Bean
	public DirectExchange ragChatDlx() {
		return new DirectExchange("rag.chat.dlx");
	}

	@Bean
	public Queue ragChatDlq() {
		return QueueBuilder.durable(queueName + ".dlq").build();
	}

	@Bean
	public Binding ragChatJobBinding() {
		return BindingBuilder.bind(ragChatJobQueue())
			.to(ragChatExchange()).with("rag.chat.job");
	}

	@Bean
	public Binding ragChatDlqBinding() {
		return BindingBuilder.bind(ragChatDlq())
			.to(ragChatDlx()).with("rag.chat.job.dead");
	}

	@Bean
	public MessageConverter ragChatJsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
