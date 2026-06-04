package com.ai_playground.springai_mcp_server.resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springaicommunity.mcp.annotation.McpResource;

/**
 * Exposes files from one configured local directory as read-only MCP resources.
 */
@Component
public class LocalResourceProvider {

	private final Path resourceRoot;

	public LocalResourceProvider(@Value("${app.resources.directory:./local-resources}") String resourceDirectory) {
		this.resourceRoot = Path.of(resourceDirectory).toAbsolutePath().normalize();
	}

	/**
	 * Lists the document names available from the configured local resource directory.
	 */
	@McpResource(uri = "local://catalog", name = "Local resource catalog",
			description = "Lists the local documents available from this MCP server", mimeType = "text/plain")
	public String catalog() throws IOException {
		if (Files.notExists(resourceRoot)) {
			return "No local resources are available. Expected directory: " + resourceRoot;
		}

		try (var paths = Files.list(resourceRoot)) {
			List<String> resources = paths.filter(Files::isRegularFile)
				.map(Path::getFileName)
				.map(Path::toString)
				.sorted(Comparator.naturalOrder())
				.toList();

			return resources.isEmpty() ? "No local resources are available." : String.join(System.lineSeparator(), resources);
		}
	}

	/**
	 * Reads one named document while preventing access outside the configured resource directory.
	 */
	@McpResource(uri = "local://documents/{name}", name = "Local document",
			description = "Reads a named document from the MCP server's sandboxed local resource directory")
	public String document(String name) throws IOException {
		Path requestedFile = resourceRoot.resolve(name).normalize();
		if (!resourceRoot.equals(requestedFile.getParent())) {
			throw new IllegalArgumentException("Resource must be a direct file inside the configured resource directory.");
		}
		if (!Files.isRegularFile(requestedFile)) {
			throw new IllegalArgumentException("Local resource does not exist: " + name);
		}

		return Files.readString(requestedFile, StandardCharsets.UTF_8);
	}
}
