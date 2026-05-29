package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.rag.data.DocumentPreview;
import com.ai_playground.springai_langchian4j.rag.data.DocumentPreviewResponse;

@RestController
@RequestMapping("/vector")
public class VectorController {
	
	private final EmbeddingModel embeddingModel;
	private final VectorStore vectorStore;

	public VectorController(EmbeddingModel embeddingModel, VectorStore vectorStore) {
		this.embeddingModel = embeddingModel;
		this.vectorStore = vectorStore;
	}
	
	@GetMapping("/text")
	public Map<String, Object> embedText(@RequestParam String text) {
		float[] vector = this.embeddingModel.embed(text);
		return Map.of("text", text, "vectorSize", vector.length);
	}
	
	@GetMapping("/query")
	public DocumentPreviewResponse query(@RequestParam String query) {
		List<Document> documents = this.vectorStore.similaritySearch(SearchRequest.builder()
			.query(query)
			.topK(5)
			.build());
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
}
