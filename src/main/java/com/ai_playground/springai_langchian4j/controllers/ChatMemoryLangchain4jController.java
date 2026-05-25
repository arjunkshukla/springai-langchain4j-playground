package com.ai_playground.springai_langchian4j.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.lc4j.InMemoryAssistant;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistantWithMessageSummarizationStrategy;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistantWithSlidingWindowStrategy;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistantWithTokenWindowStrategy;

@RestController
@RequestMapping("/chat-memory-lc4j")
public class ChatMemoryLangchain4jController {

	private final InMemoryAssistant inMemoryAssistant;
	private final SQLPersistedChatAssistantWithSlidingWindowStrategy sqlPersistedChatAssistantWithSlidingWindowStrategy;
	private final SQLPersistedChatAssistantWithTokenWindowStrategy sqlPersistedChatAssistantWithTokenWindowStrategy;
	private final SQLPersistedChatAssistantWithMessageSummarizationStrategy sqlPersistedChatAssistantWithMessageSummarizationStrategy;

	public ChatMemoryLangchain4jController(InMemoryAssistant inMemoryAssistant,
			SQLPersistedChatAssistantWithSlidingWindowStrategy sqlPersistedChatAssistantWithSlidingWindowStrategy,
			SQLPersistedChatAssistantWithTokenWindowStrategy sqlPersistedChatAssistantWithTokenWindowStrategy,
			SQLPersistedChatAssistantWithMessageSummarizationStrategy sqlPersistedChatAssistantWithMessageSummarizationStrategy) {
		this.inMemoryAssistant = inMemoryAssistant;
		this.sqlPersistedChatAssistantWithSlidingWindowStrategy = sqlPersistedChatAssistantWithSlidingWindowStrategy;
		this.sqlPersistedChatAssistantWithTokenWindowStrategy = sqlPersistedChatAssistantWithTokenWindowStrategy;
		this.sqlPersistedChatAssistantWithMessageSummarizationStrategy = sqlPersistedChatAssistantWithMessageSummarizationStrategy;
	}

	@PostMapping("/in-memory-chat")
	public String inMemoryChat(@RequestParam String sessionId, @RequestBody String message) {
		return inMemoryAssistant.chat(sessionId, message);
	}

	@PostMapping("/sql-persisted-memory-chat-with-sliding-window-strategy")
	public String sqlPersistedMemoryChatWithSlidingWindowStrategy(@RequestParam String sessionId, @RequestBody String message) {
		return sqlPersistedChatAssistantWithSlidingWindowStrategy.chat(sessionId, message);
	}
	
	@PostMapping("/sql-persisted-memory-chat-with-token-window-strategy")
	public String sqlPersistedMemoryChatWithTokenWindowStrategy(@RequestParam String sessionId, @RequestBody String message) {
		return sqlPersistedChatAssistantWithTokenWindowStrategy.chat(sessionId, message);
	}
	
	@PostMapping("/sql-persisted-memory-chat-with-message-summarization-strategy")
	public String sqlPersistedMemoryChatWithMessageSummarizationStrategy(@RequestParam String sessionId, @RequestBody String message) {
		return sqlPersistedChatAssistantWithMessageSummarizationStrategy.chat(sessionId, message);
	}
}
