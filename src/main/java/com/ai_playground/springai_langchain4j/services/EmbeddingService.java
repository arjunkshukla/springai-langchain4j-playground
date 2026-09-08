package com.ai_playground.springai_langchain4j.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.ai_playground.springai_langchain4j.rag.DocumentReader;

/**
 * Handles ingestion into the configured Spring AI vector store.
 *
 * <p>Most of the app uses the higher-level {@link VectorStore} abstraction.
 * That keeps ingestion portable across pgvector, Elasticsearch, and other
 * Spring AI vector-store implementations.</p>
 */
@Service
public class EmbeddingService {

	private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
	private final VectorStore vectorStore;
	private final ETLService etlService;

	public EmbeddingService(VectorStore vectorStore, ETLService etlService) {
		this.vectorStore = vectorStore;
		this.etlService = etlService;
	}

	/**
	 * Stores fully prepared documents through Spring AI's vector-store abstraction.
	 *
	 * <p>The store handles embedding plus persistence, which keeps the ingestion
	 * path small and consistent.</p>
	 */
	public void embedDocuments(List<Document> documents) {
		this.vectorStore.add(documents);
	}
	
	/**
	 * Reads, chunks, and stores a single classpath document using custom splitter
	 * settings.
	 */
	public void embedResource(String filename, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks, Map<String, Object> metadata) {
		Resource resource = new ClassPathResource("docs/" + filename);
		this.vectorStore.add(etlService.process(resource, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks, metadata));
	}
	
	/**
	 * Reads, chunks, and stores a single classpath document using the default
	 * splitter settings.
	 */
	public void embedResource(String filename, Map<String, Object> metadata) {
		Resource resource = new ClassPathResource("docs/" + filename);
		this.vectorStore.add(etlService.process(resource, metadata));
	}
	
	/**
	 * Scans a classpath pattern and stores every readable document it finds.
	 */
	public void embedDirectory(String pathPattern, Map<String, Object> metadata) {
		try {
			Resource[] resources = this.resourcePatternResolver.getResources(pathPattern);
			for (Resource resource : resources) {
				if (DocumentReader.isReadableDocument(resource)) {
					this.vectorStore.add(etlService.process(resource, metadata));
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load documents from classpath:/docs", ex);
		}
	}
	
	/**
	 * Same as {@link #embedDirectory(String, Map)} but with custom chunking
	 * settings.
	 */
	public void embedDirectory(String pathPattern, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks, Map<String, Object> metadata) {
		try {
			Resource[] resources = this.resourcePatternResolver.getResources(pathPattern);
			for (Resource resource : resources) {
				if (DocumentReader.isReadableDocument(resource)) {
					this.vectorStore.add(etlService.process(resource, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks, metadata));
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load documents from classpath:/docs", ex);
		}
	}
	
	/**
	 * Alias for the standard vector-store ingestion path.
	 *
	 * <p>Kept for readability in places where the caller wants the method name to
	 * say "embed and save" explicitly.</p>
	 */
	public void embedAndSaveDocuments(List<Document> documents) {
		this.vectorStore.add(documents);
	}
}
