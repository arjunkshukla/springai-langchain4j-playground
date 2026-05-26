package com.ai_playground.springai_langchian4j;

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

@Configuration
public class SpringAIConfig {

	// Default ChatClient bean without memory advisors, so the non-memory examples keep working without modification. 
	// The memory-enabled clients will be separate beans that we can inject where needed for the memory demos.
	@Bean
	@Primary
	public ChatClient chatClient(ChatClient.Builder builder) {
		// Keep the shared ChatClient plain so the non-memory examples keep working.
		// The memory demo uses a dedicated client bean with MessageChatMemoryAdvisor.
		return builder.defaultSystem("You are a helpful Java Assistant").build();
	}
	
	// This ChatMemory bean is used by the in-memory chat demo. It keeps the last 10 messages in memory for context.
	@Bean
	public ChatMemory chatMemory() {
		// Use Spring AI's current in-memory chat-memory implementation.
		// This keeps the in-memory example intact while matching the 1.1.x memory API.
		return MessageWindowChatMemory.builder()
				.maxMessages(10)//Configure the Advisor's memory to keep the last 10 messages in context. Adjust as needed for your use case.
				.build();
	}

	// This ChatClient bean is configured with a MessageChatMemoryAdvisor that uses the in-memory ChatMemory. 
	// This allows the in-memory chat demo to automatically have memory capabilities without needing to manage the memory manually in the controller.
	@Bean("memoryChatClient")
	public ChatClient memoryChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
		return builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build()).build();
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
