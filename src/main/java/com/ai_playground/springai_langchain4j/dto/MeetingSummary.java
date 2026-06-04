package com.ai_playground.springai_langchain4j.dto;

import java.util.List;

public record MeetingSummary(String mainTopic, List<ActionItem> actionItems) {

}
