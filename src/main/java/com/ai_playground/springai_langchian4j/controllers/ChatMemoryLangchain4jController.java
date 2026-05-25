package com.ai_playground.springai_langchian4j.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.lc4j.InMemoryAssistant;
import com.ai_playground.springai_langchian4j.lc4j.SQLPersistedChatAssistant;

@RestController
@RequestMapping("/chat-memory-lc4j")
public class ChatMemoryLangchain4jController {

	private final InMemoryAssistant inMemoryAssistant;
	private final SQLPersistedChatAssistant sqlPersistedChatAssistant;
	
	public ChatMemoryLangchain4jController(InMemoryAssistant inMemoryAssistant, SQLPersistedChatAssistant sqlPersistedChatAssistant) {
		this.inMemoryAssistant = inMemoryAssistant;
		this.sqlPersistedChatAssistant = sqlPersistedChatAssistant;
	}
	
	@PostMapping("/in-memory-chat")
	public String inMemoryChat(@RequestParam String sessionId, @RequestBody String message) {
		return inMemoryAssistant.chat(sessionId, message);
	}
	
	@PostMapping("/sql-persisted-memory-chat")
	public String sqlPersistedMemoryChat(@RequestParam String sessionId, @RequestBody String message) {
		return sqlPersistedChatAssistant.chat(sessionId, message);
	}
}
