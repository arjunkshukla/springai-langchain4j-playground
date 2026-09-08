package com.ai_playground.springai_langchain4j;

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

import com.ai_playground.springai_langchain4j.config.ElasticsearchVectorStoreTestConfiguration;

/**
 * End-to-end check that documents can be stored in Elasticsearch and retrieved back
 * with semantic search.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(ElasticsearchVectorStoreTestConfiguration.class)
class ElasticsearchVectorStoreRagIntegrationTest {

	@Autowired
	private VectorStore vectorStore;

	/**
	 * Stores a couple of sample documents and confirms the most relevant one is
	 * returned by similarity search.
	 */
	@Test
	void storesAndRetrievesDocumentsFromElasticsearch() {
		this.vectorStore.add(List.of(
				new Document("Spring AI integrates with Elasticsearch as a vector store."),
				new Document("Cats enjoy naps and warm sunlight.")));

		List<Document> results = this.vectorStore.similaritySearch(
				SearchRequest.builder().query("elasticsearch vector store").topK(1).build());

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getText()).contains("Elasticsearch");
	}
}
