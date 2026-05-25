package com.ai_playground.springai_langchian4j.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conversation")
public class ManualMemoryManagementController {

	private final ChatClient chatClient;
	private final List<Message> conversationHistory = new ArrayList<>();

	public ManualMemoryManagementController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@GetMapping("/no-memory-management")
	public String noMemoryManagement(@RequestParam String input) {
		return chatClient.prompt().user(input).call().content();
	}
	
	@GetMapping("/with-memory-management")
	public String withMemoryManagement(@RequestParam String input) {
		conversationHistory.add(new UserMessage(input));
		
		ChatResponse chatResponse1 = chatClient.prompt().messages(conversationHistory).call().chatResponse();
		
		AssistantMessage aiResposne1 = chatResponse1.getResult().getOutput();
		conversationHistory.add(aiResposne1);
		return aiResposne1.getText();
	}
}
