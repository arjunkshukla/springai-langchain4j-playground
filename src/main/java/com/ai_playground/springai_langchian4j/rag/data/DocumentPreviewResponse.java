package com.ai_playground.springai_langchian4j.rag.data;

import java.util.List;

public record DocumentPreviewResponse(int totalDocuments, List<DocumentPreview> documents) {
}
