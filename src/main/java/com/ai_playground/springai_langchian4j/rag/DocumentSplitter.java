package com.ai_playground.springai_langchian4j.rag;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.ai_playground.springai_langchian4j.splitter.MarkdownSectionSplitter;

@Component
public class DocumentSplitter {

	private final DocumentReader documentReader;

	public DocumentSplitter(DocumentReader documentReader) {
		this.documentReader = documentReader;
	}

	public List<Document> split(Resource resource) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		TokenTextSplitter splitter = new TokenTextSplitter();
		return splitter.apply(rawDocuments);
	}

	public List<Document> split(List<Document> rawDocuments) {
		TokenTextSplitter splitter = new TokenTextSplitter();
		return splitter.apply(rawDocuments);
	}

	public List<Document> split(List<Document> rawDocuments, int chunkSize, int minChunkSizeChars,
			int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
				maxNumChunks, keepSeparator, punctuationMarks);
		return splitter.apply(rawDocuments);
	}

	public List<Document> split(Resource resource, int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed,
			int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		List<Document> rawDocuments = this.documentReader.tikaReader(resource);
		TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
				maxNumChunks, keepSeparator, punctuationMarks);
		return splitter.apply(rawDocuments);
	}
	
	//CombiningStrategies can be implemented here as well, for example, a method that takes a list of raw documents and applies different splitting strategies based on the document type or content. For simplicity, we will just implement the token-based splitter with customizable parameters.
	// The below shows both approaches i.e Format Aware Splitting and Recursive Character Splitting combined together.
	public List<Document> advanceMarkdownSplitter(List<Document> rawDocuments, int chunkSize, int minChunkSizeChars,
			int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
		
		MarkdownSectionSplitter markdownSplitter = new MarkdownSectionSplitter();
		List<Document> sections = markdownSplitter.apply(rawDocuments);
		
		TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, minChunkSizeChars, minChunkLengthToEmbed,
				maxNumChunks, keepSeparator, punctuationMarks);
		return splitter.apply(sections);
	}
}
