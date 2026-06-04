package com.ai_playground.springai_mcp_server.tools;

import org.springframework.stereotype.Component;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

/**
 * Provides small deterministic text utilities that MCP clients can invoke.
 */
@Component
public class TextTools {

	/**
	 * Counts the words in the supplied text without requiring an AI model.
	 */
	@McpTool(name = "wordCount", description = "Counts the words in a block of text.")
	public int wordCount(
			@McpToolParam(description = "Text whose words should be counted.", required = true) String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}

		return text.trim().split("\\s+").length;
	}
}
