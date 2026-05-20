package com.ai_playground.springai_langchian4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openaisdk.OpenAiSdkChatModel;
import org.springframework.ai.openaisdk.OpenAiSdkChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MultiModelConfig {

	@Value("${spring.ai.openai-sdk.base-url:https://api.openai.com/v1}")
	private String openAiBaseUrl;

	@Value("${spring.ai.openai-sdk.api-key:}")
	private String openAiApiKey;

	@Value("${spring.ai.openai-sdk.chat.options.model:gpt-5-mini}")
	private String openAiModel;

	@Value("${spring.ai.ollama.base-url:http://localhost:11434}")
	private String ollamaBaseUrl;

	@Value("${spring.ai.ollama.chat.options.model:llama3}")
	private String ollamaModel;

	@Bean
	@Primary
	public ChatClient chatClient(OpenAiSdkChatModel openAiSdkChatModel) {
		return ChatClient.create(openAiSdkChatModel);
	}

	@Bean("ollamaChatClient")
	public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
		return ChatClient.create(ollamaChatModel);
	}

	@Bean
	public OllamaChatModel ollamaChatModel() {
		OllamaApi ollamaApi = OllamaApi.builder()
				.baseUrl(ollamaBaseUrl)
				.build();

		return OllamaChatModel.builder()
				.ollamaApi(ollamaApi)
				.defaultOptions(OllamaChatOptions.builder()
						.model(ollamaModel)
						.build())
				.build();
	}

	@Bean
	public OpenAiSdkChatModel openAiSdkChatModel() {
		return OpenAiSdkChatModel.builder()
				.options(OpenAiSdkChatOptions.builder()
						.baseUrl(openAiBaseUrl)
						.apiKey(openAiApiKey)
						.model(openAiModel)
						.build())
				.build();
	}
}
