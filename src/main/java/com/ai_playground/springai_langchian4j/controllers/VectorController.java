package com.ai_playground.springai_langchian4j.controllers;

import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vector")
public class VectorController {

	private final EmbeddingModel embeddingModel;

	public VectorController(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
	}

	@GetMapping("/embed")
	public Map<String, Object> embedText(@RequestParam String text) {
		float[] vector = this.embeddingModel.embed(text);
		return Map.of("text", text, "vectorSize", vector.length);
	}

}
