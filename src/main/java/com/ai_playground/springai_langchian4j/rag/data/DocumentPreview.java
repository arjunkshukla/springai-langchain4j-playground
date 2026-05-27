package com.ai_playground.springai_langchian4j.rag.data;

import java.util.Map;

import org.springframework.ai.document.Document;

public record DocumentPreview(String source, int characterCount, String preview, Map<String, Object> metadata) {

	public static DocumentPreview from(Document document) {
		String content = document.getFormattedContent();
		String preview = content == null ? "" : content.replaceAll("\\s+", " ");
		if (preview.length() > 240) {
			preview = preview.substring(0, 240) + "...";
		}
		return new DocumentPreview(String.valueOf(document.getMetadata().getOrDefault("source", "unknown")),
				content == null ? 0 : content.length(), preview, document.getMetadata());
	}
}