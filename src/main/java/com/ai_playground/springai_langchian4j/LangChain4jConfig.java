package com.ai_playground.springai_langchian4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ai_playground.springai_langchian4j.lc4j.ChatMessageEntityRepository;
import com.ai_playground.springai_langchian4j.lc4j.InMemoryAssistant;
import com.ai_playground.springai_langchian4j.lc4j.PersistentChatMemoryStore;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistant;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

@Configuration
public class LangChain4jConfig {
	@Bean
	public ChatMemoryProvider memoryProvider() {
		return memory -> MessageWindowChatMemory.withMaxMessages(10);
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
	@Bean
	public ChatMemoryProvider persistentMemoryProvider(ChatMemoryStore persistentChatMemoryStore) {
		return memoryId -> MessageWindowChatMemory.builder()
				.id(memoryId)
				.maxMessages(10)
				.chatMemoryStore(persistentChatMemoryStore)
				.build();
	}
	
	@Bean
	public SQLPersistedChatAssistant sqlPersistedChatAssistant(
			ChatModel langchain4jOllamaChatModel,
			ChatMemoryStore persistentChatMemoryStore,
			ChatMemoryProvider persistentMemoryProvider) {
		
		return AiServices.builder(SQLPersistedChatAssistant.class)
				.chatModel(langchain4jOllamaChatModel)
				.chatMemoryProvider(persistentMemoryProvider)
				.build();
	}
}
