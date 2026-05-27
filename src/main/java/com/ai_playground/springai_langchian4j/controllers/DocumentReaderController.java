package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.rag.DocumentReader;
import com.ai_playground.springai_langchian4j.rag.data.DocumentPreview;
import com.ai_playground.springai_langchian4j.rag.data.DocumentPreviewResponse;

@RestController
@RequestMapping("/document-reader")
public class DocumentReaderController {

	private final DocumentReader documentReader;

	public DocumentReaderController(DocumentReader simpleIngestion) {
		this.documentReader = simpleIngestion;
	}

	@GetMapping("/text")
	public DocumentPreviewResponse text() {
		List<Document> documents = this.documentReader.textReaderSample();
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
	
	@GetMapping("/pdf")
	public DocumentPreviewResponse pdf() {
		List<Document> documents = this.documentReader.pdfReaderSample();
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
	
	@GetMapping("/directory")
	public DocumentPreviewResponse documents() {
		List<Document> documents = this.documentReader.directoryReaderSample();
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
}
