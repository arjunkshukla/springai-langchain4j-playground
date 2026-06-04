package com.ai_playground.springai_langchain4j.services;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

/**
 * Encapsulates retrieval-augmented generation concerns for the app.
 *
 * <p>The service owns two pieces of the RAG pipeline: vector retrieval with
 * metadata filters and the final streamed answer generation.</p>
 */
@Service
public class RAGService {

	private final VectorStore vectorStore;
	private final ChatClient persistedChatClient;
	
	public RAGService(VectorStore vectorStore, ChatClient persistedChatClient) {
		this.vectorStore = vectorStore;
		this.persistedChatClient = persistedChatClient;
	}
	
	/**
	 * Retrieves only the chunks allowed by tenant and clearance metadata.
	 *
	 * <p>This is the important security hook in the demo: even if a document is a
	 * semantic match, it is filtered out unless the metadata matches the caller's
	 * tenant and clearance values.</p>
	 */
	public String retrieveContext(String message, int topK, double similarityThreshold, String tenantId, String clearance) {
		FilterExpressionBuilder fb = new FilterExpressionBuilder();
		Expression filterExpression = fb.and(fb.eq("tenant_id", tenantId), fb.eq("clearance", clearance))// Filtering Chunks based on tenant_id and clearance level, which are stored as metadata in the vector store.
				.build();
		List<Document> documents = this.vectorStore.similaritySearch(SearchRequest.builder()
				.query(message)
				.topK(topK)//This is the number of top similar documents to retrieve from the vector store. 
						   //You can adjust this based on your needs and the size of your document collection. 
						   //A common choice is between 3 to 5, but you can experiment with different values to see what works best for your application.
				.similarityThreshold(similarityThreshold)//This helps to enable Hybrid search by enable Keyword Search using BM25 algorithm and then filter the results using the similarity threshold. 
										//This way, we can get relevant results even if they are not very similar in vector space, as long as they match the keywords in the query.
				.filterExpression(filterExpression)
				.build());

		return documents.stream()
			.map(Document::getText)
			.collect(Collectors.joining("\n\n---\n\n"));
	}

	/**
	 * Streams the final assistant answer back to the HTTP response body.
	 *
	 * <p>Chunks are written as they arrive so the browser can render a live
	 * typing-style response instead of waiting for the whole generation to
	 * finish.</p>
	 */
	public void streamChat(String sessionId, String message, String retrievedContext, OutputStream outputStream) throws IOException {
		var responseStream = persistedChatClient.prompt()
				.system("""
						You are a helpful assistant using retrieved context from a vector store.
						Answer only from the context below.
						If the context does not contain the answer, say that you do not know.

						Context:
						%s
						""".formatted(retrievedContext))
				.user(message)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
				.stream()
				.content();

		CountDownLatch finished = new CountDownLatch(1);
		AtomicReference<Throwable> error = new AtomicReference<>();

		// Subscribe to the reactive stream and push each chunk directly to the HTTP
		// response so the UI can update incrementally.
		responseStream.subscribe(
				chunk -> {
					try {
						outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
						outputStream.flush();
					}
					catch (IOException ex) {
						error.set(ex);
						finished.countDown();
					}
				},
				ex -> {
					error.set(ex);
					finished.countDown();
				},
				finished::countDown);

		try {
			finished.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while streaming chat response", ex);
		}

		Throwable thrown = error.get();
		if (thrown != null) {
			if (thrown instanceof IOException ioException) {
				throw ioException;
			}
			throw new IOException("Failed while streaming chat response", thrown);
		}
	}
}
