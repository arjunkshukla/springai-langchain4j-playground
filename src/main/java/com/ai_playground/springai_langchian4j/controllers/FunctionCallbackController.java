package com.ai_playground.springai_langchian4j.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/function-callback")
public class FunctionCallbackController {

	private final ChatClient chatClient;

	public FunctionCallbackController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	@GetMapping("/ask")
	public String askAI(@RequestParam String question) {
		var promptRequest = chatClient.prompt().user(question);
		
		List<ToolCallback> toolCallbacks = new ArrayList<>();
		
		
		
		return chatClient.prompt().user(question).call().content();
	}
}
