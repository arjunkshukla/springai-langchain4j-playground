package com.ai_playground.springai_langchian4j.lc4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface InMemoryAssistant {

	@SystemMessage("You are a helpful customer support agent")
	String chat(@MemoryId String userId, @UserMessage String message);
}
