package com.ai_playground.springai_langchian4j.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-memory-springai")
public class ChatMemorySpringAIController {

	private final ChatClient chatClient;
	private final ChatClient memoryChatClient;
	private final ChatClient persistedChatClient;
	private final List<Message> conversationHistory = new ArrayList<>();

	public ChatMemorySpringAIController(@Qualifier("chatClient") ChatClient chatClient, 
			@Qualifier("memoryChatClient") ChatClient memoryChatClient,
			@Qualifier("persistedChatClient") ChatClient persistedChatClient) {
		this.chatClient = chatClient;
		this.memoryChatClient = memoryChatClient;
		this.persistedChatClient = persistedChatClient;
	}
	
	@GetMapping("/no-memory-management")
	public String noMemoryManagement(@RequestParam String input) {
		return chatClient.prompt().user(input).call().content();
	}
	
	@GetMapping("/manual-memory-management")
	public String manualMemoryManagement(@RequestParam String input) {
		conversationHistory.add(new UserMessage(input));
		
		ChatResponse chatResponse1 = chatClient.prompt().messages(conversationHistory).call().chatResponse();
		
		AssistantMessage aiResposne1 = chatResponse1.getResult().getOutput();
		conversationHistory.add(aiResposne1);
		return aiResposne1.getText();
	}

	@PostMapping("/in-memory-chat")
	public String inMemoryChat(@RequestParam String sessionId, @RequestBody String message) {
		// Spring AI's MessageChatMemoryAdvisor requires a conversation id per request.
		// We pass it here so the same session can pick up previous messages from the in-memory ChatMemory.
		return memoryChatClient.prompt()
				.user(message)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
				.call()
				.content();
	}
	
	@PostMapping("/persisted-memory-chat")
	public String persistedMemoryChat(@RequestParam String sessionId, @RequestBody String message) {
		// Spring AI's MessageChatMemoryAdvisor requires a conversation id per request.
		// We pass it here so the same session can pick up previous messages from the in-memory ChatMemory.
		return persistedChatClient.prompt()
				.user(message)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
				.call()
				.content();
	}

}
