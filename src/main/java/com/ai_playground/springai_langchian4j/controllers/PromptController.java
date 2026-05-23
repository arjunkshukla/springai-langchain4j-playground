package com.ai_playground.springai_langchian4j.controllers;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.renderer.SpelPromptTemplateRenderer;

@RestController
@RequestMapping("/prompt")
public class PromptController {

	private final ChatClient chatClient;
	private final SpelPromptTemplateRenderer templateRenderer;
	
	@Value("classpath:/prompt/prompt_template.st")
	private Resource templateResource;
	
	@Value("classpath:/prompt/user-analysis.st")
	private Resource userAnalysisTemplateResource;

	public PromptController(ChatClient chatClient, SpelPromptTemplateRenderer templateRenderer) {
		this.chatClient = chatClient;
		this.templateRenderer = templateRenderer;
	}
	
	@GetMapping("/inline")
	public String inlineString(@RequestParam String topic) {
		PromptTemplate template = PromptTemplate.builder().template("Tell me a joke about {topic}").build();
		Prompt prompt = template.create(Map.of("topic", topic));
		
		return chatClient.prompt(prompt)// Initiates the prompt-building process for a chat interaction with the AI.
				.call()// Executes the request synchronously, sending the constructed prompt to the AI
						// and waiting for a response. This blocks the thread until the AI responds.
				.content();// Extracts the content of the AI's response as String body discarding any
							// additional metadata or information that may be included in the response
							// object like tokens used, response time, etc.
	}
	
	@GetMapping("/read-from-template-file")
	public String readFromFile(@RequestParam String topic, @RequestParam(defaultValue = "50") Integer length) {
		PromptTemplate template = PromptTemplate.builder().resource(templateResource).build();
		Prompt prompt = template.create(Map.of("topic", topic,"length", length));
		
		return chatClient.prompt(prompt)// Initiates the prompt-building process for a chat interaction with the AI.
				.call()// Executes the request synchronously, sending the constructed prompt to the AI
						// and waiting for a response. This blocks the thread until the AI responds.
				.content();// Extracts the content of the AI's response as String body discarding any
							// additional metadata or information that may be included in the response
							// object like tokens used, response time, etc.
	}
	
	@GetMapping("/using-spel")
	public String usingSpelRenderer(@RequestParam String name, @RequestParam(defaultValue = "true") Boolean isPremium, @RequestParam(defaultValue = "50") Integer loginCount) {
		PromptTemplate template = PromptTemplate.builder()
				.resource(userAnalysisTemplateResource)
				.renderer(templateRenderer)
				.build();

		Prompt prompt = template.create(Map.of(
				"name", name,
				"isPremium", isPremium,
				"loginCount", loginCount));

		return chatClient.prompt(prompt)
				.call()
				.content();
	}
}
