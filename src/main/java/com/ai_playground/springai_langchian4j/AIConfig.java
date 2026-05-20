package com.ai_playground.springai_langchian4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

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

	@Bean(name = "langchain4jChatModel")
	public ChatModel langchain4jChatModel(
			@Value("${app.langchain4j.openai.api-key}") String apiKey,
			@Value("${app.langchain4j.openai.model-name}") String modelName) {
		return OpenAiChatModel.builder()
				.apiKey(apiKey)
				.modelName(modelName)
				.build();
	}

	@Bean
	public LangChain4jAssistant langchain4jAssistant(ChatModel langchain4jChatModel) {
		return AiServices.create(LangChain4jAssistant.class, langchain4jChatModel);
	}
}
