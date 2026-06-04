package com.ai_playground.springai_langchain4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ai_playground.springai_langchain4j.tools.DemoTools;

/**
 * Central Spring AI configuration for regular chat and tool-enabled chat clients.
 */
@Configuration
public class AIConfig {

	/**
	 * Creates the primary chat client for normal prompt/response interactions.
	 */
	@Bean
	@Primary
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.defaultSystem("You are a helpful Java Assistant").build();
	}

	/**
	 * Creates a chat client with statically registered tools from {@link DemoTools}.
	 */
	@Bean
	public ChatClient toolChatClient(ChatClient.Builder builder, DemoTools demoTools) {
		return builder.defaultSystem("""
				You are a helpful assistant.
				When a tool is used, treat the tool result as the authoritative source for that topic.
				Preserve all factual values from tool results exactly.
				Do not replace tool facts with your own knowledge or inferred live data.
				If the user asks multiple questions, answer every part of the request.
				Do not mention that a tool was called unless the user asks.
				""").defaultTools(demoTools).build();
	}

	// Using Persistence with Spring AI's ChatMemoryRepository and JdbcChatMemoryRepository is optional, but it lets us verify that the chat memory is actually being persisted to the local Postgres database instead of just kept in memory.
	// This also keeps the Spring AI memory demo intact while swapping out the in-memory store for a JDBC-backed repository.
	@Bean
	public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
		// Spring AI's JDBC chat-memory repository uses JdbcTemplate directly, which lets us
		// test persistence against the local Postgres database without introducing another
		// custom persistence layer for the Spring AI example.
		return JdbcChatMemoryRepository.builder()
				.jdbcTemplate(jdbcTemplate)
				.build();
	}

	// This ChatMemory bean is used by the persisted chat demo.
	// It uses the JdbcChatMemoryRepository to store messages in Postgres, so the memory persists across application restarts.
	@Bean
	public ChatMemory persistedChatMemory(ChatMemoryRepository chatMemoryRepository) {
		// This keeps the Spring AI memory demo intact, but swaps the backing store from
		// a volatile in-memory map to the JDBC repository so we can verify persistence.
		return MessageWindowChatMemory.builder()
				.maxMessages(10)
				.chatMemoryRepository(chatMemoryRepository)
				.build();
	}

	// This ChatClient bean is configured with a MessageChatMemoryAdvisor that uses the persisted ChatMemory.
	// This allows the persisted chat demo to automatically have memory capabilities with durable storage, without needing to manage the memory manually in the controller.
	@Bean("persistedChatClient")
	public ChatClient persistedChatClient(ChatClient.Builder builder, ChatMemory persistedChatMemory) {
		return builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(persistedChatMemory).build()).build();
	}
}
