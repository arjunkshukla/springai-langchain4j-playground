package com.ai_playground.springai_langchian4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ai_playground.springai_langchian4j.rag.DocumentReader;

@Configuration
public class AIConfig {

	// This method defines a Spring Bean for the ChatClient,
	// which is used to interact with the AI.
	// The ChatClient is configured with a default system prompt that sets the
	// context for the AI,
	// instructing it to act as a helpful Java Assistant.
	// This means that any interactions with the AI will be framed within this
	// context,
	// guiding the AI's responses to be relevant to Java programming assistance.
	@Bean
	public ChatClient chatClient(ChatClient.Builder builder) {// The ChatClient.Builder is injected into the method,
																// allowing us to configure the ChatClient before it is
																// built.
		return builder.defaultSystem("You are a helpful Java Assistant").build();// The builder is used to set a default
																					// system prompt for the ChatClient,
																					// and then the build() method is
																					// called to create the ChatClient
																					// instance that will be managed by
																					// Spring.
	}

	@Bean
	public DocumentReader simpleIngestion() {
		return new DocumentReader();
	}
}
