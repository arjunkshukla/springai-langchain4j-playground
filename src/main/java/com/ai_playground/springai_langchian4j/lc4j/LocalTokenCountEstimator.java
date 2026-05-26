package com.ai_playground.springai_langchian4j.lc4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.TokenCountEstimator;

public class LocalTokenCountEstimator implements TokenCountEstimator {

	@Override
	public int estimateTokenCountInText(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		return text.trim().split("\\s+|(?=[\\p{Punct}])|(?<=[\\p{Punct}])").length;
	}

	@Override
	public int estimateTokenCountInMessage(ChatMessage message) {
		int tokenCount = 4;
		if (message instanceof SystemMessage systemMessage) {
			tokenCount += estimateTokenCountInText(systemMessage.text());
		} else if (message instanceof UserMessage userMessage) {
			if (userMessage.hasSingleText()) {
				tokenCount += estimateTokenCountInText(userMessage.singleText());
			} else {
				for (var content : userMessage.contents()) {
					tokenCount += estimateTokenCountInText(content.toString());
				}
			}
		} else if (message instanceof AiMessage aiMessage) {
			tokenCount += estimateTokenCountInText(aiMessage.text());
		} else if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
			tokenCount += estimateTokenCountInText(toolExecutionResultMessage.text());
		} else {
			tokenCount += estimateTokenCountInText(message.toString());
		}
		return tokenCount;
	}

	@Override
	public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
		int tokenCount = 3;
		for (ChatMessage message : messages) {
			tokenCount += estimateTokenCountInMessage(message);
		}
		return tokenCount;
	}

}
