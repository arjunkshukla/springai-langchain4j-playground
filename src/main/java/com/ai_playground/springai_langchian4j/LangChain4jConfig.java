package com.ai_playground.springai_langchian4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ai_playground.springai_langchian4j.lc4j.ChatMessageEntityRepository;
import com.ai_playground.springai_langchian4j.lc4j.LocalTokenCountEstimator;
import com.ai_playground.springai_langchian4j.lc4j.InMemoryAssistant;
import com.ai_playground.springai_langchian4j.lc4j.MessageSummaryChatMemory;
import com.ai_playground.springai_langchian4j.lc4j.PersistentChatMemoryStore;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistantWithMessageSummarizationStrategy;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistantWithSlidingWindowStrategy;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistantWithTokenWindowStrategy;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

@Configuration
public class LangChain4jConfig {
	private static final int DEFAULT_MAX_MESSAGES = 10;
	private static final int DEFAULT_MAX_TOKENS = 1000;

	@Bean
	public ChatMemoryProvider memoryProvider() {
		return memory -> MessageWindowChatMemory.withMaxMessages(DEFAULT_MAX_MESSAGES);
	}
	
	// Keep Spring AI Ollama auto-configured, but create the LangChain4j Ollama
	// model manually with a unique bean name to avoid the bean-definition
	// collision between Spring AI's `ollamaChatModel` and the LangChain4j starter.
	@Bean("langchain4jOllamaChatModel")
	public ChatModel langchain4jOllamaChatModel(
			@Value("${langchain4j.ollama.chat-model.base-url}") String baseUrl,
			@Value("${langchain4j.ollama.chat-model.model-name}") String modelName) {
		return OllamaChatModel.builder()
				.baseUrl(baseUrl)
				.modelName(modelName)
				.build();
	}
	
	@Bean
	public InMemoryAssistant inMemoryAssistant(ChatModel langchain4jOllamaChatModel, ChatMemoryProvider memoryProvider) {
		return AiServices.builder(InMemoryAssistant.class)
				.chatModel(langchain4jOllamaChatModel)
				.chatMemoryProvider(memoryProvider)
				.build();
	}
	
	@Bean
	public ChatMemoryStore persistentChatMemoryStore(ChatMessageEntityRepository chatMessageEntityRepository) {
		return new PersistentChatMemoryStore(chatMessageEntityRepository);
	}
	
	// This assistant needs durable memory, so we build a dedicated chat memory per
	// conversation id and attach the persistent store here. This keeps the SQL-backed
	// memory separate from the in-memory assistant and lets LangChain4j reload prior
	// messages for the same session.
	// Sliding Window Strategy - This provider keeps a fixed number of the most recent messages in context. 
	// It's simple and effective for many use cases, but may lose important context in longer conversations if earlier messages are relevant. 
	// Adjust the maxMessages parameter as needed to balance context retention and token limits.
	@Bean
	public ChatMemoryProvider persistentMemoryProvider(ChatMemoryStore persistentChatMemoryStore) {
		return memoryId -> MessageWindowChatMemory.builder()
				.id(memoryId)
				.maxMessages(DEFAULT_MAX_MESSAGES)
				.chatMemoryStore(persistentChatMemoryStore)
				.build();
	}
	
	@Bean
	public SQLPersistedChatAssistantWithSlidingWindowStrategy sqlPersistedChatAssistantWithSlidingWindowStrategy(
			ChatModel langchain4jOllamaChatModel,
			ChatMemoryStore persistentChatMemoryStore,
			ChatMemoryProvider persistentMemoryProvider) {
		
		return AiServices.builder(SQLPersistedChatAssistantWithSlidingWindowStrategy.class)
				.chatModel(langchain4jOllamaChatModel)
				.chatMemoryProvider(persistentMemoryProvider)
				.build();
	}
	
	// Token Window Strategy - Instead of keeping a fixed number of messages, this provider keeps as many messages as possible within a token limit. 
	// This is more flexible and can provide better context retention for longer conversations, especially when messages vary in length.
	@Bean
	public ChatMemoryProvider tokenWindowProvider(ChatMemoryStore persistentChatMemoryStore) {
		return memory -> TokenWindowChatMemory.builder()
				.id(memory)
				.maxTokens(DEFAULT_MAX_TOKENS, tokenCountEstimator())
				.chatMemoryStore(persistentChatMemoryStore)
				.build();
	}
	
	@Bean
	public TokenCountEstimator tokenCountEstimator() {
		// LangChain4j 1.11 does not ship a generic tokenizer for Ollama here, so we use a
		// local token estimator that keeps token-window memory functional without adding
		// another provider-specific dependency.
		return new LocalTokenCountEstimator();
	}
	
	@Bean
	public SQLPersistedChatAssistantWithTokenWindowStrategy sqlPersistedChatAssistantWithTokenWindowStrategy(
			ChatModel langchain4jOllamaChatModel,
			ChatMemoryStore persistentChatMemoryStore,
			ChatMemoryProvider tokenWindowProvider) {
		
		return AiServices.builder(SQLPersistedChatAssistantWithTokenWindowStrategy.class)
				.chatModel(langchain4jOllamaChatModel)
				.chatMemoryProvider(tokenWindowProvider)
				.build();
	}
	
	// Message Summarization Strategy - This provider summarizes the conversation history when a message or token limit is exceeded, allowing it to retain important context while keeping the working set small.
	@Bean
	public ChatMemoryProvider tokenSummarizationProvider(ChatMemoryStore persistentChatMemoryStore,
			ChatModel langchain4jOllamaChatModel) {
		// LangChain4j does not ship a built-in MessageSummaryChatMemory in this version,
		// so we provide the summarization strategy locally while keeping the same bean
		// shape the rest of the app already expects.
		return memoryId -> new MessageSummaryChatMemory(
				memoryId,
				DEFAULT_MAX_MESSAGES,
				langchain4jOllamaChatModel,
				persistentChatMemoryStore);
	}
	
	@Bean
	public SQLPersistedChatAssistantWithMessageSummarizationStrategy sqlPersistedChatAssistantWithMessageSummarizationStrategy(
			ChatModel langchain4jOllamaChatModel,
			ChatMemoryStore persistentChatMemoryStore,
			ChatMemoryProvider tokenSummarizationProvider) {
		
		return AiServices.builder(SQLPersistedChatAssistantWithMessageSummarizationStrategy.class)
				.chatModel(langchain4jOllamaChatModel)
				.chatMemoryProvider(tokenSummarizationProvider)
				.build();
	}
}
