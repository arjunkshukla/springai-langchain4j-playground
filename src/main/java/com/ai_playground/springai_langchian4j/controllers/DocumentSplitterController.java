package com.ai_playground.springai_langchian4j.controllers;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ai_playground.springai_langchian4j.rag.DocumentSplitter;
import com.ai_playground.springai_langchian4j.rag.data.DocumentPreview;
import com.ai_playground.springai_langchian4j.rag.data.DocumentPreviewResponse;

@RestController
@RequestMapping("/document-splitter")
public class DocumentSplitterController {

	@Value("classpath:docs/04-reference.pdf")
	private Resource pdfResource;
	
	private final DocumentSplitter documentSplitter;

	public DocumentSplitterController(DocumentSplitter documentSplitter) {
		this.documentSplitter = documentSplitter;
	}

	@GetMapping("/split")
	public DocumentPreviewResponse split() {
		List<Document> documents = this.documentSplitter.split(pdfResource);
		List<DocumentPreview> preview = documents.stream()
			.map(DocumentPreview::from)
			.toList();
		return new DocumentPreviewResponse(documents.size(), preview);
	}
	
}
