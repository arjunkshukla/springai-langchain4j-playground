package com.ai_playground.springai_langchain4j.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchain4j.rag.data.DocumentPreview;
import com.ai_playground.springai_langchain4j.rag.data.DocumentPreviewResponse;
import com.ai_playground.springai_langchain4j.services.ETLService;

/**
 * Lets you inspect how the ingestion pipeline reads and splits documents.
 */
@RestController
@RequestMapping("/etl")
public class ETLController {
	
	
	private final ETLService etlService;

	public ETLController(ETLService etlService) {
		this.etlService = etlService;
	}

	/**
	 * Runs the standard token-based ETL pipeline against a classpath document.
	 */
	@GetMapping
	public DocumentPreviewResponse transform(
			@RequestParam(defaultValue = "04-reference.pdf") String filename,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks,
			@RequestBody(required = false)  Map<String, Object> metadata
			) {
		Resource resource = new ClassPathResource("docs/" + filename);
		List<Document> documents = etlService.process(resource, chunkSize, minChunkSizeChars,
				minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks, metadata);
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
	
	/**
	 * Runs the markdown-aware ETL pipeline so markdown headings stay visible in
	 * the resulting chunks.
	 */
	@GetMapping("/markdown")
	public DocumentPreviewResponse transformMarkdown(
			@RequestParam(defaultValue = "04-reference.md") String filename,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks,
			@RequestBody(required = false)  Map<String, Object> metadata) {
		Resource resource = new ClassPathResource("docs/" + filename);
		List<Document> documents = etlService.markdownProcess(resource, chunkSize, minChunkSizeChars,
				minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks, metadata);
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
}
