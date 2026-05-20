package com.ai_playground.springai_langchian4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LangChain4jAssistant {

	@SystemMessage("You are a comedian. Be sarcastic and funny.")
	@UserMessage("Tell me a joke about {{topic}}")
	String tellJoke(@V("topic") String topic);
}
