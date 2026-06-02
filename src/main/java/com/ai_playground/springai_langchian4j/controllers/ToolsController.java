package com.ai_playground.springai_langchian4j.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tools")
public class ToolsController {

	private final ChatClient chatClient;

	public ToolsController(@Qualifier("toolChatClient") ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@GetMapping("/ask")
	public String askAI(@RequestParam String question) {
		return chatClient.prompt().user(question).call().content();
	}
}
