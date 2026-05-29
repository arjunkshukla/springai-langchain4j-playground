package com.ai_playground.springai_langchian4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIConfig {

	// This method defines a Spring Bean for the ChatClient,
	// which is used to interact with the AI.
	// The ChatClient is configured with a default system prompt that sets the
	// context for the AI,
	// instructing it to act as a helpful Java Assistant.
	// This means that any interactions with the AI will be framed within this
	// context,
	// guiding the AI's responses to be relevant to Java programming assistance.
	@Bean
	@Primary
	public ChatClient chatClient(ChatClient.Builder builder) {// The ChatClient.Builder is injected into the method,
																// allowing us to configure the ChatClient before it is
																// built.
		return builder.defaultSystem("You are a helpful Java Assistant").build();// The builder is used to set a default
																					// system prompt for the ChatClient,
																					// and then the build() method is
																					// called to create the ChatClient
																					// instance that will be managed by
																					// Spring.
	}

	// This ChatMemory bean is used by the persisted chat demo.
	// It uses the JdbcChatMemoryRepository to store messages in Postgres, so the
	// memory persists across application restarts.
	@Bean
	public ChatMemory persistedChatMemory(ChatMemoryRepository chatMemoryRepository) {
		// This keeps the Spring AI memory demo intact, but swaps the backing store from
		// a volatile in-memory map to the JDBC repository so we can verify persistence.
		return MessageWindowChatMemory.builder().maxMessages(10).chatMemoryRepository(chatMemoryRepository).build();
	}

	// This ChatClient bean is configured with a MessageChatMemoryAdvisor that uses
	// the persisted ChatMemory.
	// This allows the persisted chat demo to automatically have memory capabilities
	// with durable storage, without needing to manage the memory manually in the
	// controller.
	@Bean("persistedChatClient")
	public ChatClient persistedChatClient(ChatClient.Builder builder, ChatMemory persistedChatMemory) {
		return builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(persistedChatMemory).build()).build();
	}
}
