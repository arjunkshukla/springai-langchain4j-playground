package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.ai_playground.springai_langchian4j.services.QueryRewriteService;
import com.ai_playground.springai_langchian4j.services.RAGService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/rag")
public class RagController {

	private final ChatClient chatClient;
	private final RAGService ragService;
	private final VectorStore vectorStore;
	private final ChatClient persistedChatClient;
	private final QueryRewriteService queryRewriteService;
	
	public RagController(ChatClient chatClient, VectorStore vectorStore,
			@Qualifier("persistedChatClient") ChatClient persistedChatClient, QueryRewriteService queryRewriteService, RAGService ragService) {
		this.chatClient = chatClient;
		this.vectorStore = vectorStore;
		this.persistedChatClient = persistedChatClient;
		this.queryRewriteService = queryRewriteService;
		this.ragService = ragService;
	}
	
	@GetMapping("/ask")
	public String ask(@RequestParam String question,
			@RequestParam(defaultValue = "4") int topK,
			@RequestParam(defaultValue = "0.0") double similarityThreshold) {
		List<Document> documents = this.vectorStore.similaritySearch(SearchRequest.builder()
			.query(question)
			.topK(topK)
			.similarityThreshold(similarityThreshold)
			.build());

		String retrievedContext = documents.stream()
			.map(Document::getText)
			.collect(Collectors.joining("\n\n---\n\n"));

		return this.chatClient.prompt()
			.system("""
				You are a helpful assistant using retrieved context from a vector store.
				Answer only from the context below.
				If the context does not contain the answer, say that you do not know.

				Context:
				%s
				""".formatted(retrievedContext))
			.user(question)
			.call()
			.content();
	}
	
	@PostMapping("/chat")
	public String persistedMemoryChat(@RequestParam String sessionId, @RequestBody String message, @RequestParam(defaultValue = "4") int topK, @RequestParam(defaultValue = "0.0") double similarityThreshold) {
		log.info("Received message for session {}: {}", sessionId, message);
		String newMessage = queryRewriteService.rewrite(sessionId, message);
		log.info("Rewritten message for session {}: {}", sessionId, newMessage);
		
		String retrievedContext = ragService.retrieveContext(newMessage, topK, similarityThreshold);

		// Spring AI's MessageChatMemoryAdvisor requires a conversation id per request.
		// We pass it here so the same session can pick up previous messages from the in-memory ChatMemory.
		return persistedChatClient.prompt()
				.system("""
						You are a helpful assistant using retrieved context from a vector store.
						Answer only from the context below.
						If the context does not contain the answer, say that you do not know.

						Context:
						%s
						""".formatted(retrievedContext))
				.user(newMessage)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
				.call()
				.content();
	}

	@PostMapping(value = "/chat/stream", produces = "text/plain;charset=UTF-8")
	public StreamingResponseBody persistedMemoryChatStream(@RequestParam String sessionId, @RequestBody String message, @RequestParam(defaultValue = "4") int topK, @RequestParam(defaultValue = "0.0") double similarityThreshold) {		
		String retrievedContext = ragService.retrieveContext(message, topK, similarityThreshold);
		return outputStream -> ragService.streamChat(sessionId, message, retrievedContext, outputStream);
	}

	
}
