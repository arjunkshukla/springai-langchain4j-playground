package com.ai_playground.springai_langchian4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.ai_playground.springai_langchian4j.config.PgVectorTestConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(PgVectorTestConfiguration.class)
class PgVectorRagIntegrationTest {

	@Autowired
	private VectorStore vectorStore;

	@Test
	void storesAndRetrievesDocumentsFromPgVector() {
		this.vectorStore.add(List.of(
				new Document("Spring AI integrates with PostgreSQL and pgvector."),
				new Document("Cats enjoy naps and warm sunlight.")));

		List<Document> results = this.vectorStore.similaritySearch(
				SearchRequest.builder().query("postgres vector store").topK(1).build());

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getText()).contains("pgvector");
	}
}
