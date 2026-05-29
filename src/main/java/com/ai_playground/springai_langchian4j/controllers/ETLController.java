package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.rag.data.DocumentPreview;
import com.ai_playground.springai_langchian4j.rag.data.DocumentPreviewResponse;
import com.ai_playground.springai_langchian4j.services.ETLService;

@RestController
@RequestMapping("/etl")
public class ETLController {
	
	
	private final ETLService etlService;

	public ETLController(ETLService etlService) {
		this.etlService = etlService;
	}

	@GetMapping
	public DocumentPreviewResponse transform(
			@RequestParam(defaultValue = "04-reference.pdf") String filename,
			@RequestParam(defaultValue = "etl") String category,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks) {
		Resource resource = new ClassPathResource("docs/" + filename);
		List<Document> documents = etlService.process(resource, category, chunkSize, minChunkSizeChars,
				minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks);
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
	

	@GetMapping("/markdown")
	public DocumentPreviewResponse transformMarkdown(
			@RequestParam(defaultValue = "04-reference.md") String filename,
			@RequestParam(defaultValue = "etl") String category,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks) {
		Resource resource = new ClassPathResource("docs/" + filename);
		List<Document> documents = etlService.markdownProcess(resource, category, chunkSize, minChunkSizeChars,
				minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks);
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
}
