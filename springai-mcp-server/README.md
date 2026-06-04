# springai-mcp-server

Standalone Spring Boot application that demonstrates MCP resources, tools, and prompts with Spring AI.

It is intentionally separate from the existing `springai-langchain4j` application. The MCP server does not require an LLM or an API key.

## Resources

| MCP resource URI | Purpose |
| --- | --- |
| `local://catalog` | Lists files available from the configured resource directory |
| `local://documents/{name}` | Reads one named local file |

Only direct files inside the configured resource directory are exposed. Requests that attempt to leave that directory are rejected.

## Tool

| MCP tool | Purpose |
| --- | --- |
| `wordCount` | Counts words in supplied text without invoking an AI model |

Example arguments:

```json
{
  "text": "Spring AI MCP server"
}
```

## Prompt

| MCP prompt | Purpose |
| --- | --- |
| `reviewLocalDocument` | Creates review instructions for one document exposed by the server |

The prompt accepts a required `name` argument, such as `server-guide.md`, and directs the client to read `local://documents/server-guide.md` before reviewing it.

## Run

From the repository's `codebase` directory:

```powershell
cd springai-mcp-server
mvn spring-boot:run
```

The Streamable HTTP MCP endpoint is available at:

```text
http://localhost:8081/mcp
```

## Configure Local Resources

The default resource directory is:

```text
springai-mcp-server/local-resources
```

To expose a different directory:

```powershell
$env:MCP_RESOURCE_DIRECTORY = "C:\path\to\documents"
mvn spring-boot:run
```

The server treats resources as read-only and only reads direct files from the configured directory.

## Example MCP Client Configuration

Configure an MCP client that supports Streamable HTTP with:

```json
{
  "name": "springai-mcp-server",
  "url": "http://localhost:8081/mcp"
}
```

After connecting, the client can:

- list and read resources
- invoke the `wordCount` tool
- retrieve the `reviewLocalDocument` prompt

## Project Structure

```text
springai-mcp-server/
|-- local-resources/                         Sample local resources
|-- src/main/java/.../LocalResourceProvider.java
|-- src/main/java/.../LocalResourcePrompts.java
|-- src/main/java/.../TextTools.java
|-- src/main/resources/application.properties
|-- src/test/java/...                         Resource, tool, and prompt tests
`-- pom.xml
```

## Security Boundary

This example intentionally does not expose arbitrary filesystem paths. `LocalResourceProvider` resolves every requested filename against one configured root and verifies that the normalized path remains inside that root.
