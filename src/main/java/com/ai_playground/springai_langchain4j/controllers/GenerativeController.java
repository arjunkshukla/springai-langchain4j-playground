package com.ai_playground.springai_langchain4j.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tiny "hello world" style chat endpoints for exploring the raw chat client.
 */
@RestController
public class GenerativeController {

	private final ChatClient chatClient;

	public GenerativeController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Sends a one-off prompt to the assistant with no RAG context.
	 */
	@GetMapping("/ask")
	public String askAI(@RequestParam String question) {
		return chatClient.prompt()
				.user(question)
				.call()
				.content();
	}

	/**
	 * Demonstrates prompt shaping with a custom system instruction.
	 */
	@GetMapping("/joke")
	public String tellJoke(@RequestParam String topic) {
		return chatClient.prompt()
				.system("You are a comedian. Be sarcastic and funny.")
				.user(u -> u.text("Tell me a joke about {topic}")
						.param("topic", topic))
				.call()
				.content();
	}
}
