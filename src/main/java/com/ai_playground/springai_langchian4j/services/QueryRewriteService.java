package com.ai_playground.springai_langchian4j.services;

import java.util.List;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

@Service
public class QueryRewriteService {

	private final ChatMemory persistedChatMemory;
	private final ChatClient chatClient;
	
	private static final String REWRITE_PROMPT_TEMPLATE = """
		You are a helpful assistant that rewrites user queries to be more clear and concise, based on the conversation history.
		Conversation history:
		{conversation_history}
	
		Check if the following User Query needs context from the Conversation History to be understood.
	
		If the query is standalone (e.g. "Whatis Java?"), return it exactly as is.
		If the query relies on history (e.g. "How does it compare?"), rewrite it to be the a standalone sentence including the necessary context.
	
		User query:
		{user_query}
	
		Do NOT answer the question. Just return the rewritten query.
	""";


	public QueryRewriteService(ChatMemory persistedChatMemory, ChatClient chatClient) {
		this.persistedChatMemory = persistedChatMemory;
		this.chatClient = chatClient;
	}

	public String rewrite(String sessionId, String query) {
		List<Message> conversations = persistedChatMemory.get(sessionId);
		if(null == conversations || conversations.isEmpty()) {
			return query;// If there is no conversation history, return the original query as is.
		}
		

		String conversationHistory = conversations.stream()
				.map(msg -> "%s: %s".formatted(msg.getMessageType(), msg.getText())).reduce((a, b) -> a + "\n" + b)
				.orElse("");
		Question question = chatClient.prompt()
				.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
				.system(REWRITE_PROMPT_TEMPLATE.replace("{conversation_history}", conversationHistory)
						.replace("{user_query}", query))
				.call()
				.entity(Question.class);
		
		
		return question.question();
	}
	

	public record Question(String question) {
	
	}
}
