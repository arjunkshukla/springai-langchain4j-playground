package com.ai_playground.springai_langchain4j.dto;

public record ActionItem(String assignee,
		String taskDescription,
		String dueDate,
		Priority priority) {
}

