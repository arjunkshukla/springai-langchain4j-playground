package com.ai_playground.springai_langchian4j.services;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.List;
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

import com.ai_playground.springai_langchian4j.rag.DocumentReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;

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

	// This method both embeds the documents and saves them to the vector store, which in this case is backed by PostgreSQL with pgvector.
	public void embedDocuments(List<Document> documents) {
		this.vectorStore.add(documents);
	}
	
	public void embedResource(String filename, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		Resource resource = new ClassPathResource("docs/" + filename);
		this.vectorStore.add(etlService.process(resource, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks));
	}
	
	public void embedResource(String filename) {
		Resource resource = new ClassPathResource("docs/" + filename);
		this.vectorStore.add(etlService.process(resource));
	}
	
	public void embedDirectory(String pathPattern) {
		try {
			Resource[] resources = this.resourcePatternResolver.getResources(pathPattern);
			for (Resource resource : resources) {
				if (DocumentReader.isReadableDocument(resource)) {
					this.vectorStore.add(etlService.process(resource));
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load documents from classpath:/docs", ex);
		}
	}
	
	public void embedDirectory(String pathPattern, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		try {
			Resource[] resources = this.resourcePatternResolver.getResources(pathPattern);
			for (Resource resource : resources) {
				if (DocumentReader.isReadableDocument(resource)) {
					this.vectorStore.add(etlService.process(resource, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks));
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load documents from classpath:/docs", ex);
		}
	}
	
	// This method both embeds the documents and saves them to the vector store, which in this case is backed by PostgreSQL with pgvector.
	public void embedAndSaveDocuments(List<Document> documents) {
		this.vectorStore.add(documents);
	}

	// This method demonstrates how to embed documents and save them to PostgreSQL with pgvector directly using JdbcTemplate, 
	// without going through the VectorStore abstraction.
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

	private String qualifiedTableName() {
		return this.schemaName + "." + this.tableName;
	}

	private String toJson(Object value) {
		try {
			return this.objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize metadata for pgvector insert", ex);
		}
	}
}
