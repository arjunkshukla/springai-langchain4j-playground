package com.ai_playground.springai_langchian4j.dto;

public record ActionItem(String assignee,
		String taskDescription,
		String dueDate,
		Priority priority) {
}

