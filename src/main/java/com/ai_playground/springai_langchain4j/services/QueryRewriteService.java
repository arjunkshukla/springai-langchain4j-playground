package com.ai_playground.springai_langchain4j.services;

import java.util.List;

import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

/**
 * Rewrites follow-up user questions into standalone search queries.
 *
 * <p>This is the query-understanding step that runs before vector retrieval so
 * a vague follow-up question can still use the right context from chat
 * history.</p>
 */
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

	/**
	 * Rewrites a question using the persisted conversation history for the given
	 * session.
	 *
	 * <p>If there is no history, the original query is returned unchanged. When
	 * history exists, the model is instructed to return only the rewritten query
	 * text, not an answer.</p>
	 */
	public String rewrite(String sessionId, String query) {
		List<Message> conversations = persistedChatMemory.get(sessionId);
		if(null == conversations || conversations.isEmpty()) {
			return query;
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
	
	/**
	 * Minimal structured response used to coerce the rewrite model into returning
	 * only the rewritten query text.
	 */
	public record Question(String question) {
	
	}
}
