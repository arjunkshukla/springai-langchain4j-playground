package com.ai_playground.springai_langchian4j.services;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

	private final EmbeddingModel embeddingModel;
	
	@Value("${spring.ai.ollama.embedding.model:nomic-embed-text}")
	private String embeddingModelName;
	
	public EmbeddingService(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
	}
	
	public void embedDocuments(List<Document> documents) {
		//Extract just the text content from each document
		List<String> texts = documents.stream()
				.map(Document::getText)
				.toList();
		
		//Create a batch Request
		EmbeddingRequest request = new EmbeddingRequest(texts, EmbeddingOptions.builder()
				.model(embeddingModelName)
				.build());
		
		// Execute the batch embedding request
		EmbeddingResponse response = this.embeddingModel.call(request);
		
		for (int i = 0; i < documents.size(); i++) {
			float[] vector = response.getResults().get(i).getOutput();
		}
	}
}
