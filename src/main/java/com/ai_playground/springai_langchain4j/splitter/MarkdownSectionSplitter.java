package com.ai_playground.springai_langchain4j.splitter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

/**
 * Custom document transformer that preserves markdown section boundaries.
 *
 * <p>The demo uses this to keep heading-based sections intact before token
 * splitting so the resulting chunks retain their markdown context.</p>
 */
public class MarkdownSectionSplitter implements DocumentTransformer {

	/**
	 * Splits each document at markdown level-2 headings while preserving the
	 * original metadata.
	 *
	 * <p>The split regex uses a lookahead so the heading stays attached to the
	 * section that follows it, which is usually what we want for retrieval.</p>
	 */
	@Override
	public List<Document> apply(List<Document> documents) {
		List<Document> splitDocs = new ArrayList<>();
		for (Document doc : documents) {
			String content = doc.getText();
			String[] sections = content.split("(?=\n## )");
			for (String section : sections) {
				Document splitDoc = new Document(section.trim(), doc.getMetadata());
				splitDoc.getMetadata().put("split_type", "markdown_header");
				splitDoc.getMetadata().putAll(doc.getMetadata());
				splitDocs.add(splitDoc);
			}
		}
		return splitDocs;
	}

}
