package com.ai_playground.springai_langchian4j.splitter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;

public class MarkdownSectionSplitter implements DocumentTransformer {

	@Override
	public List<Document> apply(List<Document> documents) {
		List<Document> splitDocs = new ArrayList<>();
		for (Document doc : documents) {
			String content = doc.getText();
			String[] sections = content.split("(?=\n## )"); // Split on markdown headers
			for (String section : sections) {
				Document splitDoc = new Document(section.trim(), doc.getMetadata()); // Create new document with same metadata
				splitDoc.getMetadata().put("split_type","markdown_header"); // Preserve metadata
				splitDoc.getMetadata().putAll(doc.getMetadata()); // Preserve all original metadata
				splitDocs.add(splitDoc);
			}
		}
		return splitDocs;
	}

}
