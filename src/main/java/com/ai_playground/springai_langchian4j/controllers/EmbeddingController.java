package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.services.EmbeddingService;

@RestController
@RequestMapping("/embed")
public class EmbeddingController {

	private final EmbeddingService embeddingService;

	public EmbeddingController(EmbeddingService embeddingService) {
		this.embeddingService = embeddingService;
	}

	@GetMapping("/file")
	public ResponseEntity<HttpStatus> embedFile(@RequestParam String filename,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks) {
		this.embeddingService.embedResource(filename, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks,
				keepSeparator, punctuationMarks);
		return ResponseEntity.ok(HttpStatus.OK);
	}

	@GetMapping("/directory")
	public ResponseEntity<HttpStatus> embedDirectory(@RequestParam String dirPath,
			@RequestParam(defaultValue = "800") int chunkSize,
			@RequestParam(defaultValue = "350") int minChunkSizeChars,
			@RequestParam(defaultValue = "5") int minChunkLengthToEmbed,
			@RequestParam(defaultValue = "10000") int maxNumChunks,
			@RequestParam(defaultValue = "true") boolean keepSeparator,
			@RequestParam(defaultValue = ".,?,!") List<Character> punctuationMarks) {
		this.embeddingService.embedDirectory(dirPath, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks,
				keepSeparator, punctuationMarks);
		return ResponseEntity.ok(HttpStatus.OK);
	}

}
