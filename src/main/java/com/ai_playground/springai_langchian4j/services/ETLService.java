package com.ai_playground.springai_langchian4j.services;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.ai_playground.springai_langchian4j.rag.DocumentReader;
import com.ai_playground.springai_langchian4j.rag.DocumentSplitter;

@Service
public class ETLService {
	
	private final DocumentReader documentReader;
	private final DocumentSplitter documentSplitter;
	
	public ETLService(DocumentReader documentReader, DocumentSplitter documentSplitter) {
		this.documentReader = documentReader;
		this.documentSplitter = documentSplitter;
	}

	public List<Document> process(Resource resource) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		for (Document doc : rawDocuments) {
			doc.getMetadata().put("source", resource.getFilename());
		}
		return this.documentSplitter.split(rawDocuments);
	}
	
	public List<Document> process(Resource resource, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		for (Document doc : rawDocuments) {
			doc.getMetadata().put("source", resource.getFilename());
		}
		return this.documentSplitter.split(rawDocuments, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks);
	}
	
	public List<Document> markdownProcess(Resource resource, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		for (Document doc : rawDocuments) {
			doc.getMetadata().put("source", resource.getFilename());
		}
		return this.documentSplitter.advanceMarkdownSplitter(rawDocuments, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks);
	}
}
