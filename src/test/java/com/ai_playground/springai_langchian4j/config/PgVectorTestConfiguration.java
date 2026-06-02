package com.ai_playground.springai_langchian4j.config;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test-only configuration that wires pgvector and a deterministic embedding
 * model for repeatable integration tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PgVectorTestConfiguration {

	/**
	 * Starts a pgvector-enabled PostgreSQL container and exposes it through Spring
	 * Boot's service connection support.
	 */
	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> pgvectorContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
			.asCompatibleSubstituteFor("postgres"))
			.withDatabaseName("rag")
			.withUsername("test")
			.withPassword("test");
	}

	/**
	 * Replaces the production embedding model with a tiny deterministic model so
	 * the test can assert retrieval behavior without calling Ollama.
	 */
	@Bean
	@Primary
	EmbeddingModel testEmbeddingModel() {
		return new EmbeddingModel() {

			@Override
			public EmbeddingResponse call(EmbeddingRequest request) {
				List<Embedding> results = request.getInstructions().stream()
					.map(this::embedText)
					.map(vector -> new Embedding(vector, 0))
					.toList();
				return new EmbeddingResponse(results);
			}

			@Override
			public float[] embed(Document document) {
				return embedText(document.getText());
			}

			@Override
			public float[] embed(String text) {
				return embedText(text);
			}

			@Override
			public String getEmbeddingContent(Document document) {
				return document.getText();
			}

			@Override
			public int dimensions() {
				return 4;
			}

			private float[] embedText(String text) {
				String value = text == null ? "" : text.toLowerCase();
				return new float[] {
						score(value, "spring", "java"),
						score(value, "postgres", "pgvector", "vector", "database"),
						score(value, "rag", "retrieval", "search"),
						score(value, "cat", "pet", "animal")
				};
			}

			private float score(String value, String... keywords) {
				for (String keyword : keywords) {
					if (value.contains(keyword)) {
						return 1.0f;
					}
				}
				return 0.0f;
			}
		};
	}
}
