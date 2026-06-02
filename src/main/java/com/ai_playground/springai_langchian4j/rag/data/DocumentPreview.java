package com.ai_playground.springai_langchian4j.rag.data;

import java.util.Map;

import org.springframework.ai.document.Document;

/**
 * Compact preview of a Spring AI document chunk for API responses and UI
 * inspection.
 */
public record DocumentPreview(String source, int characterCount, String preview, Map<String, Object> metadata) {

	/**
	 * Converts a Spring AI document into a lightweight preview object.
	 *
	 * <p>The preview collapses whitespace and truncates long content so response
	 * payloads stay readable.</p>
	 */
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
