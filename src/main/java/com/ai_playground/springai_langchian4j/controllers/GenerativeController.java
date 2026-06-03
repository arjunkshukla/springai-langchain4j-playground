package com.ai_playground.springai_langchian4j.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes simple chat endpoints that use the primary non-tool Spring AI chat client.
 */
@RestController
public class GenerativeController {

	private final ChatClient chatClient;

	/**
	 * Creates the controller with the primary chat client.
	 */
	public GenerativeController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Sends a free-form question to the primary chat client and returns the response text.
	 */
	@GetMapping("/ask")
	public String askAI(@RequestParam String question) {
		return chatClient.prompt()
				.user(question)
				.call()
				.content();
	}

	/**
	 * Adds a comedian system prompt and asks the model to generate a joke for a topic.
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
