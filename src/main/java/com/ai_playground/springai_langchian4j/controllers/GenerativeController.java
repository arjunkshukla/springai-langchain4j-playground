package com.ai_playground.springai_langchian4j.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenerativeController {

	// The ChatClient is injected into the controller via constructor injection.
	// This allows the controller to use the ChatClient to interact with the AI for
	// generating responses based on user input.
	private final ChatClient chatClient;
	private final ChatClient ollamaChatClient;

	public GenerativeController(@Qualifier("chatClient") ChatClient chatClient, @Qualifier("ollamaChatClient") ChatClient ollamaChatClient) {
		this.chatClient = chatClient;
		this.ollamaChatClient = ollamaChatClient;
	}

	// This endpoint allows users to ask any question to the AI and get a response.
	// The question is passed as a query parameter, and the AI's response is
	// returned as a plain string.
	@GetMapping("/ask")
	public String askAI(@RequestParam String question, @RequestParam(defaultValue = "openai") String model) {
		if("ollama".equalsIgnoreCase(model)) {
			System.out.println("Using Ollama model");
			return ollamaChatClient.prompt()// Initiates the prompt-building process for a chat interaction with the AI.
					.user(question)// Sets the "User Prompt" (the actual question or request)
					.call()// Executes the request synchronously, sending the constructed prompt to the AI
							// and waiting for a response. This blocks the thread until the AI responds.
					.content();// Extracts the content of the AI's response as String body discarding any
								// additional metadata or information that may be included in the response
								// object like tokens used, response time, etc.
		} else {
			System.out.println("Using OpenAI model");
			return chatClient.prompt()// Initiates the prompt-building process for a chat interaction with the AI.
					.user(question)// Sets the "User Prompt" (the actual question or request)
					.call()// Executes the request synchronously, sending the constructed prompt to the AI
							// and waiting for a response. This blocks the thread until the AI responds.
					.content();// Extracts the content of the AI's response as String body discarding any
								// additional metadata or information that may be included in the response
								// object like tokens used, response time, etc.
		}
	}

	// This endpoint is specifically designed to generate jokes based on a given
	// topic. It sets a system prompt to instruct the AI to act as a comedian and be
	// sarcastic and funny, then constructs a user prompt asking for a joke about
	// the specified topic. The AI's response is returned as a plain string.
	@GetMapping("/joke")
	public String tellJoke(@RequestParam String topic, @RequestParam(defaultValue = "openai") String model) {
		if("ollama".equalsIgnoreCase(model)) {
			System.out.println("Using Ollama model");
			return ollamaChatClient.prompt()// Initiates the prompt-building process for a chat interaction with the AI.
					.system("You are a comedian. Be sarcastic and funny.")// Sets the "System Prompt" (sets the context for
																			// the AI, telling it to act as a comedian)
					.user(u -> u.text("Tell me a joke about {topic}")// Sets the "User Prompt" (the actual question or
																		// request, with a placeholder for the topic)
							.param("topic", topic))
					.call()// Executes the request synchronously, sending the constructed prompt to the AI
							// and waiting for a response. This blocks the thread until the AI responds.
					.content();// Extracts the content of the AI's response as String body discarding any
								// additional metadata or information that may be included in the response
								// object like tokens used, response time, etc.
		} else {
			System.out.println("Using OpenAI model");
			return chatClient.prompt()// Initiates the prompt-building process for a chat interaction with the AI.
					.system("You are a comedian. Be sarcastic and funny.")// Sets the "System Prompt" (sets the context for
																			// the AI, telling it to act as a comedian)
					.user(u -> u.text("Tell me a joke about {topic}")// Sets the "User Prompt" (the actual question or
																		// request, with a placeholder for the topic)
							.param("topic", topic))
					.call()// Executes the request synchronously, sending the constructed prompt to the AI
							// and waiting for a response. This blocks the thread until the AI responds.
					.content();// Extracts the content of the AI's response as String body discarding any
								// additional metadata or information that may be included in the response
								// object like tokens used, response time, etc.
		}
	}
}
