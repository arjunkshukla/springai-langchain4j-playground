package com.ai_playground.springai_mcp_server.prompts;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LocalResourcePromptsTest {

	@Test
	void createsReviewPromptForNamedResource() {
		String prompt = new LocalResourcePrompts().reviewLocalDocument("server-guide.md");

		assertThat(prompt).contains("local://documents/server-guide.md", "Base the review only on the resource content.");
	}
}
