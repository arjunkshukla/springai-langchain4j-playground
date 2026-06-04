package com.ai_playground.springai_langchain4j.services;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.ai_playground.springai_langchain4j.rag.DocumentReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;

/**
 * Handles ingestion into the vector store and the lower-level manual pgvector
 * JDBC path used for experimentation.
 *
 * <p>Most of the app uses the higher-level {@link VectorStore} abstraction.
 * The explicit JDBC path is kept here as a reference for how the same data maps
 * to the underlying pgvector table.</p>
 */
@Service
public class EmbeddingService {

	private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
	private final EmbeddingModel embeddingModel;
	private final VectorStore vectorStore;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final ETLService etlService;

	@Value("${spring.ai.ollama.embedding.model:nomic-embed-text}")
	private String embeddingModelName;

	@Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
	private String schemaName;

	@Value("${spring.ai.vectorstore.pgvector.table-name:spring_ai_vector_store}")
	private String tableName;

	public EmbeddingService(EmbeddingModel embeddingModel, VectorStore vectorStore, JdbcTemplate jdbcTemplate, ETLService etlService,
			ObjectMapper objectMapper) {
		this.embeddingModel = embeddingModel;
		this.vectorStore = vectorStore;
		this.jdbcTemplate = jdbcTemplate;
		this.etlService = etlService;
		this.objectMapper = objectMapper;
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

	/**
	 * Demonstrates the manual path: embed in-memory, then insert directly into the
	 * pgvector table with JDBC.
	 *
	 * <p>This is intentionally more explicit than {@link #embedDocuments(List)} so
	 * you can see how the content, metadata, and vector map to the database
	 * schema.</p>
	 */
	public void embedDocumentsLongApproach(List<Document> documents) {
		List<String> texts = documents.stream()
			.map(Document::getText)
			.toList();

		EmbeddingRequest request = new EmbeddingRequest(texts,
				EmbeddingOptions.builder().model(embeddingModelName).build());
		EmbeddingResponse response = this.embeddingModel.call(request);

		String sql = "INSERT INTO " + qualifiedTableName()
				+ " (id, content, metadata, embedding) VALUES (?, ?, ?::jsonb, ?)";

		for (int i = 0; i < documents.size(); i++) {
			Document document = documents.get(i);
			float[] vector = response.getResults().get(i).getOutput();
			String metadataJson = toJson(document.getMetadata());

			this.jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(sql);
				ps.setString(1, UUID.randomUUID().toString());
				ps.setString(2, document.getText());
				ps.setString(3, metadataJson);
				ps.setObject(4, new PGvector(vector));
				return ps;
			});
		}
	}

	/**
	 * Builds the fully qualified pgvector table name from configuration.
	 */
	private String qualifiedTableName() {
		return this.schemaName + "." + this.tableName;
	}

	/**
	 * Serializes document metadata to JSON so it can be stored in the jsonb
	 * column.
	 */
	private String toJson(Object value) {
		try {
			return this.objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize metadata for pgvector insert", ex);
		}
	}
}
