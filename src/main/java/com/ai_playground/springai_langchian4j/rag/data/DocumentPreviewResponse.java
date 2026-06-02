package com.ai_playground.springai_langchian4j.rag.data;

import java.util.List;

/**
 * Wrapper response that reports the number of documents and returns previews.
 */
public record DocumentPreviewResponse(int totalDocuments, List<DocumentPreview> documents) {
}
