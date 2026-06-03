package com.ai_playground.springai_langchian4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.ai_playground.springai_langchian4j.tools.DemoTools;

/**
 * Central Spring AI configuration for regular chat and tool-enabled chat clients.
 */
@Configuration
public class AIConfig {

	/**
	 * Creates the primary chat client for normal prompt/response interactions.
	 */
	@Bean
	@Primary
	public ChatClient chatClient(ChatClient.Builder builder) {
		return builder.defaultSystem("You are a helpful Java Assistant").build();
	}

	/**
	 * Creates a chat client with statically registered tools from {@link DemoTools}.
	 */
	@Bean
	public ChatClient toolChatClient(ChatClient.Builder builder, DemoTools demoTools) {
		return builder.defaultSystem("""
				You are a helpful assistant.
				When a tool is used, treat the tool result as the authoritative source for that topic.
				Preserve all factual values from tool results exactly.
				Do not replace tool facts with your own knowledge or inferred live data.
				If the user asks multiple questions, answer every part of the request.
				Do not mention that a tool was called unless the user asks.
				""").defaultTools(demoTools).build();
	}
}
