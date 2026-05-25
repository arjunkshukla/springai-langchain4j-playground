package com.ai_playground.springai_langchian4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SpringAIConfig {

	@Bean
	public ChatMemory chatMemory() {
		// Use Spring AI's current in-memory chat-memory implementation.
		// This keeps the in-memory example intact while matching the 1.1.x memory API.
		return MessageWindowChatMemory.builder()
				.maxMessages(10)//Configure the Advisor's memory to keep the last 10 messages in context. Adjust as needed for your use case.
				.build();
	}
	
	@Bean
	@Primary
	public ChatClient chatClient(ChatClient.Builder builder) {
		// Keep the shared ChatClient plain so the non-memory examples keep working.
		// The memory demo uses a dedicated client bean with MessageChatMemoryAdvisor.
		return builder.defaultSystem("You are a helpful Java Assistant").build();
	}

	@Bean("memoryChatClient")
	public ChatClient memoryChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
		return builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
	}
	
}
