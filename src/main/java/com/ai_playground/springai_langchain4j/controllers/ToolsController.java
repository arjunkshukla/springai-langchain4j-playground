package com.ai_playground.springai_langchain4j.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes endpoints that demonstrate statically registered Spring AI tools.
 */
@RestController
@RequestMapping("/tools")
public class ToolsController {

	private final ChatClient toolChatClient;

	/**
	 * Creates the controller with a chat client that already has tools registered.
	 */
	public ToolsController(@Qualifier("toolChatClient") ChatClient toolChatClient) {
		this.toolChatClient = toolChatClient;
	}

	/**
	 * Lets the model use the statically registered tools when answering the question.
	 */
	@GetMapping("/ask")
	public String askAI(@RequestParam String question) {
		return toolChatClient.prompt().user(question).call().content();
	}

	/**
	 * Sends the same prompt through the tool-enabled client while explicitly disabling tool execution for this call.
	 */
	@GetMapping("/disabled-tools")
	public String disabledTools(@RequestParam String question) {
		return toolChatClient.prompt()
				.user(question)
				.options(OpenAiChatOptions.builder()
						.toolChoice("none")
						.internalToolExecutionEnabled(false)
						.build())
				.call()
				.content();
	}
}
