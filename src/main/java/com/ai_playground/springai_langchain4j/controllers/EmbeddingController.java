package com.ai_playground.springai_langchain4j.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchain4j.services.EmbeddingService;

/**
 * Exposes endpoints that write documents into pgvector.
 */
@RestController
@RequestMapping("/embed")
public class EmbeddingController {

	private final EmbeddingService embeddingService;

	public EmbeddingController(EmbeddingService embeddingService) {
		this.embeddingService = embeddingService;
	}

	/**
	 * Reads one document from the classpath, splits it, and stores the chunks in
	 * the vector store.
	 */
	@GetMapping("/file")
	public ResponseEntity<HttpStatus> embedFile(@RequestParam String filename,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks,
			@RequestBody(required = false)  Map<String, Object> metadata) {
		this.embeddingService.embedResource(filename, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks,
				keepSeparator, punctuationMarks, metadata);
		return ResponseEntity.ok(HttpStatus.OK);
	}

	/**
	 * Scans a classpath directory pattern and stores every readable document it
	 * finds.
	 */
	@GetMapping("/directory")
	public ResponseEntity<HttpStatus> embedDirectory(@RequestParam String dirPath,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks,
			@RequestBody(required = false)  Map<String, Object> metadata) {
		this.embeddingService.embedDirectory(dirPath, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks,
				keepSeparator, punctuationMarks, metadata);
		return ResponseEntity.ok(HttpStatus.OK);
	}

}
