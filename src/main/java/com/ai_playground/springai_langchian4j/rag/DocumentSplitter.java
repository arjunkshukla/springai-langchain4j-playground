package com.ai_playground.springai_langchian4j.rag;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.ai_playground.springai_langchian4j.splitter.MarkdownSectionSplitter;

/**
 * Applies Spring AI splitters to raw documents before embedding.
 *
 * <p>This is where the demo experiments with token splitting and markdown-aware
 * section splitting.</p>
 */
@Component
public class DocumentSplitter {

	private final DocumentReader documentReader;

	public DocumentSplitter(DocumentReader documentReader) {
		this.documentReader = documentReader;
	}

	/**
	 * Reads the resource and splits it with the default token splitter settings.
	 */
	public List<Document> split(Resource resource) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		TokenTextSplitter splitter = new TokenTextSplitter();
		return splitter.apply(rawDocuments);
	}

	/**
	 * Splits already-read documents with the default token splitter settings.
	 */
	public List<Document> split(List<Document> rawDocuments) {
		TokenTextSplitter splitter = new TokenTextSplitter();
		return splitter.apply(rawDocuments);
	}

	/**
	 * Splits documents using the caller's token-window settings.
	 *
	 * <p>The chunk parameters are surfaced all the way up to the controller so we
	 * can experiment with different RAG chunking strategies without code changes.</p>
	 */
	public List<Document> split(List<Document> rawDocuments, int chunkSize, int minChunkSizeChars,
			int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
				maxNumChunks, keepSeparator, punctuationMarks);
		return splitter.apply(rawDocuments);
	}

	/**
	 * Reads and splits a resource in a single step using custom token settings.
	 */
	public List<Document> split(Resource resource, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
				maxNumChunks, keepSeparator, punctuationMarks);
		return splitter.apply(rawDocuments);
	}
	
	/**
	 * Applies a markdown-aware split first, then token-based chunking on each
	 * section.
	 *
	 * <p>This is the custom transformer path used when we want to preserve markdown
	 * headings as part of the retrieval context instead of flattening everything
	 * into generic token windows.</p>
	 */
	public List<Document> advanceMarkdownSplitter(List<Document> rawDocuments, int chunkSize, int minChunkSizeChars,
			int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		
		MarkdownSectionSplitter markdownSplitter = new MarkdownSectionSplitter();
		List<Document> sections = markdownSplitter.apply(rawDocuments);
		
		TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
				maxNumChunks, keepSeparator, punctuationMarks);
		return splitter.apply(sections);
	}
}
