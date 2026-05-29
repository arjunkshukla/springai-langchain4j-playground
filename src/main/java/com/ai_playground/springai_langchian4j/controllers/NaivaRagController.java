package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/naive-rag")
public class NaivaRagController {

	private final ChatClient chatClient;
	private final VectorStore vectorStore;
	
	public NaivaRagController(ChatClient chatClient, VectorStore vectorStore) {
		this.chatClient = chatClient;
		this.vectorStore = vectorStore;
	}
	
	@GetMapping("/ask")
	public String ask(@RequestParam String question,
			@RequestParam(defaultValue = "4") int topK) {
		List<Document> documents = this.vectorStore.similaritySearch(SearchRequest.builder()
			.query(question)
			.topK(topK)
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
	
}
