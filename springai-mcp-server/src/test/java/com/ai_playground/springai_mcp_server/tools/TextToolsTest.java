package com.ai_playground.springai_mcp_server.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextToolsTest {

	private final TextTools textTools = new TextTools();

	@Test
	void countsWordsSeparatedByDifferentWhitespace() {
		assertThat(textTools.wordCount("Spring AI\nMCP server")).isEqualTo(4);
	}

	@Test
	void returnsZeroForBlankText() {
		assertThat(textTools.wordCount("   ")).isZero();
	}
}
