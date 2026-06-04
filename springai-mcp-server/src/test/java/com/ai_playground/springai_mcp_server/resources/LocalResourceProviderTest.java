package com.ai_playground.springai_mcp_server.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalResourceProviderTest {

	@TempDir
	Path resourceDirectory;

	@Test
	void listsAndReadsLocalResources() throws Exception {
		Files.writeString(resourceDirectory.resolve("notes.txt"), "MCP resource content");
		LocalResourceProvider provider = new LocalResourceProvider(resourceDirectory.toString());

		assertThat(provider.catalog()).isEqualTo("notes.txt");
		assertThat(provider.document("notes.txt")).isEqualTo("MCP resource content");
	}

	@Test
	void rejectsPathsOutsideTheResourceDirectory() {
		LocalResourceProvider provider = new LocalResourceProvider(resourceDirectory.toString());

		assertThatThrownBy(() -> provider.document("../secret.txt"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("direct file");
	}
}
