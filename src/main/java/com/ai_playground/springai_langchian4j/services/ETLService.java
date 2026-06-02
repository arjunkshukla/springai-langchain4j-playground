package com.ai_playground.springai_langchian4j.services;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.ai_playground.springai_langchian4j.rag.DocumentReader;
import com.ai_playground.springai_langchian4j.rag.DocumentSplitter;

/**
 * Orchestrates the document ETL pipeline: read, enrich metadata, then split.
 *
 * <p>This service keeps the document-ingestion rules in one place so the
 * controllers can stay thin.</p>
 */
@Service
public class ETLService {
	
	private final DocumentReader documentReader;
	private final DocumentSplitter documentSplitter;
	
	public ETLService(DocumentReader documentReader, DocumentSplitter documentSplitter) {
		this.documentReader = documentReader;
		this.documentSplitter = documentSplitter;
	}

	/**
	 * Reads a resource, stamps source metadata, merges custom metadata, and
	 * performs default token splitting.
	 */
	public List<Document> process(Resource resource, Map<String, Object> metadata) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		for (Document doc : rawDocuments) {
			doc.getMetadata().put("source", resource.getFilename());
			if(metadata != null && !metadata.isEmpty()) {
				doc.getMetadata().putAll(metadata);
			}
		}
		return this.documentSplitter.split(rawDocuments);
	}
	
	/**
	 * Same as {@link #process(Resource, Map)} but exposes token splitter tuning
	 * parameters.
	 */
	public List<Document> process(Resource resource, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks, Map<String, Object> metadata) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		for (Document doc : rawDocuments) {
			doc.getMetadata().put("source", resource.getFilename());
			if(metadata != null && !metadata.isEmpty()) {
				doc.getMetadata().putAll(metadata);
			}
		}
		return this.documentSplitter.split(rawDocuments, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks);
	}
	
	/**
	 * Markdown-aware ETL path: read the file, enrich metadata, then preserve
	 * markdown sections before token chunking.
	 */
	public List<Document> markdownProcess(Resource resource, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks, Map<String, Object> metadata) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		for (Document doc : rawDocuments) {
			doc.getMetadata().put("source", resource.getFilename());
			if(metadata != null && !metadata.isEmpty()) {
				doc.getMetadata().putAll(metadata);
			}
		}
		return this.documentSplitter.advanceMarkdownSplitter(rawDocuments, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks);
	}
}
