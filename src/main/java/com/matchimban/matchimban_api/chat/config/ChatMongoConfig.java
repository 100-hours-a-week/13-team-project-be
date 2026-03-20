package com.matchimban.matchimban_api.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.matchimban.matchimban_api.chat.repository")
public class ChatMongoConfig {
}
