package com.ai_playground.springai_mcp_server.prompts;

import org.springframework.stereotype.Component;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;

/**
 * Provides reusable prompt templates that work with the server's local resources.
 */
@Component
public class LocalResourcePrompts {

	/**
	 * Creates instructions for reviewing one document exposed by this MCP server.
	 */
	@McpPrompt(name = "reviewLocalDocument",
			description = "Creates a prompt for reviewing a document exposed by this MCP server.")
	public String reviewLocalDocument(
			@McpArg(name = "name", description = "Name of the local document to review.", required = true) String name) {
		return """
				Read the MCP resource local://documents/%s.
				Summarize its purpose, identify its most important details, and list any unclear or risky statements.
				Base the review only on the resource content.
				""".formatted(name);
	}
}
