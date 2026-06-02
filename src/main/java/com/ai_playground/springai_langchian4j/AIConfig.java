package com.ai_playground.springai_langchian4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Central configuration for the Spring AI clients and chat memory used by the
 * app.
 *
 * <p>This class defines the default chat client, the persisted chat memory, and
 * the memory-aware chat client used by the RAG demo.</p>
 */
@Configuration
public class AIConfig {
	
	@Value("${chat.memory.messages.window.size:10}")
	private Integer messageWindowSize;

	/**
	 * Creates the primary chat client used by most of the demo endpoints.
	 *
	 * <p>The default system prompt keeps the assistant grounded as a general
	 * Java-focused helper unless a controller supplies a more specific system
	 * prompt.</p>
	 */
	@Bean
	@Primary
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.defaultSystem("You are a helpful Java Assistant").build();
	}

	/**
	 * Builds a bounded chat-memory window backed by the configured repository.
	 *
	 * <p>The repository persists chat history in PostgreSQL, while
	 * {@link MessageWindowChatMemory} keeps only the last few turns in the active
	 * prompt context.</p>
	 */
	@Bean
	public ChatMemory persistedChatMemory(ChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder().maxMessages(messageWindowSize).chatMemoryRepository(chatMemoryRepository).build();
	}

	/**
	 * Creates the chat client used by the persisted-memory demo.
	 *
	 * <p>The {@link MessageChatMemoryAdvisor} injects the persisted memory into
	 * each request so the assistant can continue the conversation by session ID.</p>
	 */
	@Bean("persistedChatClient")
	public ChatClient persistedChatClient(ChatClient.Builder builder, ChatMemory persistedChatMemory) {
		return builder.defaultAdvisors(MessageChatMemoryAdvisor.builder(persistedChatMemory).build()).build();
	}
}
