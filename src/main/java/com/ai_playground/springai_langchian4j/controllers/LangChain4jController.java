package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.LangChain4jAssistant;

@RestController
public class LangChain4jController {

	private final ChatModel chatModel;
	private final LangChain4jAssistant langChain4jAssistant;

	public LangChain4jController(@Qualifier("langchain4jChatModel") ChatModel chatModel,
			LangChain4jAssistant langChain4jAssistant) {
		this.chatModel = chatModel;
		this.langChain4jAssistant = langChain4jAssistant;
	}

	@GetMapping("/lc4j/ask")
	public String askAI(@RequestParam String question) {
		return chatModel.chat(question);
	}

	@GetMapping("/lc4j/joke")
	public String tellJoke(@RequestParam String topic) {
		return chatModel.chat(List.of(
				SystemMessage.from("You are a comedian. Be sarcastic and funny."),
				UserMessage.from("Tell me a joke about " + topic)))
				.aiMessage()
				.text();
	}

	@GetMapping("/lc4j/service/joke")
	public String tellJokeViaService(@RequestParam String topic) {
		return langChain4jAssistant.tellJoke(topic);
	}
}
