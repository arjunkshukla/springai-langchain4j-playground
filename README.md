# springai-langchain4j-playground

This repository is a Spring Boot playground for building and exploring a local RAG application with:

- Spring AI
- Ollama
- PostgreSQL + pgvector
- a custom document ingestion pipeline
- a small browser UI that streams answers live

The goal of the project is to make the full AI pipeline visible, not hidden. You can see how documents are read, transformed, chunked, embedded, stored, retrieved, and finally passed into a prompt for answer generation.

## What this app is for

This is not a production service. It is a learning and prototyping app that shows:

- how to connect Spring AI to a local Ollama model
- how to ingest documents from `classpath:/docs`
- how to build a custom document transformer
- how to chunk and embed documents for RAG
- how to store vectors in pgvector
- how to retrieve relevant chunks and feed them back into the model
- how to stream the model output into a simple chat UI
- how to persist chat memory in PostgreSQL

If you read the sections below, you should be able to understand the whole application without opening the code first.

## Architecture at a glance

The application has four main layers:

1. Document ingestion
2. Transformation and chunking
3. Embedding and storage
4. Retrieval and answer generation

The browser UI and the HTTP controllers sit on top of those layers.

The rough flow is:

```text
docs in classpath
  -> DocumentReader
  -> DocumentSplitter / MarkdownSectionSplitter
  -> ETLService
  -> EmbeddingService
  -> VectorStore / pgvector
  -> RagController
  -> ChatClient + retrieved context
  -> browser UI
```

## Runtime setup

The app is configured in [`src/main/resources/application.properties`](src/main/resources/application.properties) to use:

- Ollama at `http://localhost:11434`
- `llama3.2:1b` for chat
- `nomic-embed-text` for embeddings
- PostgreSQL on `jdbc:postgresql://localhost:5433/rag`
- pgvector as the Spring AI vector store backend
- JDBC chat-memory persistence for the memory demo

If you want to inspect the Docker database from pgAdmin or another client, use:

- host: `localhost`
- port: `5433`
- database: `rag`
- username: `test`
- password: `test`

## Project structure

```text
src/main/java/com/ai_playground/springai_langchian4j/
  SpringAILangChain4jApplication.java
  AIConfig.java
  controllers/
    HomeController.java
    GenerativeController.java
    RagController.java
    VectorController.java
    DocumentReaderController.java
    DocumentSplitterController.java
    ETLController.java
    EmbeddingController.java
  rag/
    DocumentReader.java
    DocumentSplitter.java
    data/
      DocumentPreview.java
      DocumentPreviewResponse.java
  services/
    ETLService.java
    EmbeddingService.java
  splitter/
    MarkdownSectionSplitter.java

src/main/resources/
  application.properties
  coredeux-entities.yml
  docs/
  static/
    index.html
    app.js
    styles.css

src/test/java/com/ai_playground/springai_langchian4j/
  SpringAILangChain4jApplicationTests.java
  PgVectorRagIntegrationTest.java
  config/PgVectorTestConfiguration.java

compose.yaml
```

## Dependencies

The main runtime uses:

- Spring Boot 3.5.14
- Spring AI 1.1.6
- LangChain4j 0.35.0
- Spring Web
- Spring AI pgvector starter
- Spring AI JDBC chat-memory repository
- Coredeux Spring Boot starters

The build targets Java 21.

## Configuration explained

### Ollama

These properties tell Spring AI where Ollama is running and which models to use:

```properties
spring.ai.model.chat=ollama
spring.ai.model.embedding=ollama
spring.ai.model.audio.speech=none
spring.ai.model.audio.transcription=none
spring.ai.model.image=none
spring.ai.model.moderation=none
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.2:1b
spring.ai.ollama.embedding.options.model=nomic-embed-text
spring.ai.ollama.embedding.model=nomic-embed-text
```

What these mean:

- `spring.ai.model.chat=ollama` selects Ollama for chat generation.
- `spring.ai.model.embedding=ollama` selects Ollama for embeddings.
- `spring.ai.ollama.base-url` points Spring AI at the local Ollama server.
- `llama3.2:1b` is the small chat model used for quick local responses.
- `nomic-embed-text` is used to create embeddings for vector search.

### PostgreSQL / pgvector

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/rag
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver

spring.ai.vectorstore.pgvector.initialize-schema=true
spring.ai.vectorstore.pgvector.schema-name=public
spring.ai.vectorstore.pgvector.table-name=spring_ai_vector_store
spring.ai.vectorstore.pgvector.distance-type=cosine-distance
spring.ai.vectorstore.pgvector.index-type=hnsw
spring.ai.vectorstore.pgvector.dimensions=768
```

What these mean:

- the database runs in Docker and is exposed on host port `5433`
- Spring AI uses the `public.spring_ai_vector_store` table
- vectors are compared using cosine distance
- the table is initialized automatically
- the embedding size is set to `768`, which matches the Ollama embedding model

### Chat memory repository

```properties
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

These settings support the persisted chat-memory demo.

- `spring.ai.chat.memory.repository.jdbc.initialize-schema=always` tells Spring AI to create the JDBC chat-memory tables if needed.
- The JPA settings are present because the project uses shared Spring/PostgreSQL infrastructure and the examples keep the database ready for persistent features.

## Class-by-class guide

### `SpringAILangChain4jApplication`

This is the Spring Boot entry point.

What it does:

- bootstraps the application
- triggers component scanning
- starts the embedded web server

There is no custom logic here. It is the standard `main` method that launches Spring Boot.

### `AIConfig`

This class defines the AI-related Spring beans.

#### `chatClient(ChatClient.Builder builder)`

- builds the default `ChatClient`
- sets the default system prompt to `You are a helpful Java Assistant`
- marks that bean as `@Primary` so it is the default `ChatClient` when multiple clients exist

#### `persistedChatMemory(ChatMemoryRepository chatMemoryRepository)`

- creates a `MessageWindowChatMemory`
- stores messages through the JDBC-backed `ChatMemoryRepository`
- keeps only the latest 10 messages in memory

This is the core of the persisted memory demo.

#### `persistedChatClient(ChatClient.Builder builder, ChatMemory persistedChatMemory)`

- builds a second `ChatClient`
- attaches `MessageChatMemoryAdvisor`
- uses the persisted chat-memory bean

This lets the RAG chat endpoint keep conversation context across requests.

### `GenerativeController`

This is the simplest chat controller.

#### `GET /ask?question=...`

- sends the raw question directly to the chat model
- does not retrieve documents
- does not use RAG

#### `GET /joke?topic=...`

- sets a playful system prompt
- asks the model for a joke about the given topic

This controller is useful as a baseline for “plain chat without retrieval.”

### `HomeController`

This controller maps `/` to the static UI.

#### `GET /`

- forwards to `/index.html`
- makes the chat UI the first thing you see when you open the app

### `RagController`

This is the main RAG controller.

#### `GET /rag/ask`

- accepts a `question`
- performs a similarity search in pgvector
- joins the retrieved document text into a context block
- asks the chat model to answer only from that context

This is the basic synchronous RAG flow.

#### `POST /rag/chat`

- accepts `sessionId` as a request parameter
- accepts the chat message body in the request body
- retrieves context from pgvector
- uses the persisted chat client so the conversation can continue across requests

This endpoint is the non-streaming persistent-memory version.

#### `POST /rag/chat/stream`

- same retrieval and memory behavior as `/rag/chat`
- returns a `StreamingResponseBody`
- writes model tokens to the response as they are produced

This is the endpoint used by the browser UI.

#### `retrieveContext(String message, int topK)`

- does the vector similarity search
- returns the retrieved text joined with separators

This helper keeps the retrieval logic in one place.

#### `streamChat(...)`

- creates a streaming Spring AI chat response
- subscribes to the token stream
- writes each chunk directly to the response output stream

This is what makes the UI update live instead of waiting for a full response.

### `VectorController`

This controller is a small vector-search demo.

#### `GET /vector/text?text=...`

- embeds the text with the configured embedding model
- returns the vector size

This is mainly a diagnostic endpoint.

#### `GET /vector/query?query=...`

- runs a similarity search in pgvector
- returns document previews

This is useful for checking whether your vectors were stored correctly.

### `DocumentReaderController`

This controller shows how Spring AI readers turn files into `Document` objects.

#### `GET /document-reader/text`

- reads `coredeux-import.txt` using `TextReader`

#### `GET /document-reader/pdf`

- reads `01-overview.pdf` using `TikaDocumentReader`

#### `GET /document-reader/directory`

- scans everything under `classpath*:docs/*`
- reads each readable document with Tika

This controller is helpful for seeing what the raw ingestion output looks like before chunking.

### `DocumentSplitterController`

This controller demonstrates splitting a single document into chunks.

#### `GET /document-splitter/split`

- loads `04-reference.pdf`
- splits it using the default `TokenTextSplitter`

This is the “plain splitter” example.

### `ETLController`

This is the configurable transform endpoint.

#### `GET /etl`

- loads a file from `classpath:/docs`
- applies the default token splitter
- accepts chunking parameters as query parameters

#### `GET /etl/markdown`

- loads a markdown file from `classpath:/docs`
- uses the markdown-aware pipeline in `ETLService`

This controller is where the chunking parameters are easiest to experiment with.

### `EmbeddingController`

This controller triggers storage into pgvector.

#### `GET /embed/file`

- reads one file
- chunks it with the supplied splitter settings
- stores the resulting documents through `EmbeddingService`

#### `GET /embed/directory`

- processes every readable document under the supplied classpath pattern
- stores all chunks into pgvector

This is the main ingestion entry point for the RAG demo.

### `DocumentReader`

This is the document-loading helper.

#### `textReader(Resource resource)`

- uses `TextReader`
- adds the `source` metadata
- returns Spring AI `Document` objects

#### `tikaReader(Resource resource)`

- uses `TikaDocumentReader`
- lets Tika detect the content type automatically

#### `directoryReader(String pathPattern)`

- scans the classpath with `PathMatchingResourcePatternResolver`
- filters out unreadable resources
- sorts them by filename
- reads each resource using Tika

#### `isReadableDocument(Resource resource)`

- simple guard method for existence and readability

#### `resourceName(Resource resource)`

- gives each resource a stable sort/display name

### `DocumentSplitter`

This class is the document transformation and chunking helper.

#### `split(Resource resource)`

- reads the resource with Tika
- applies the default `TokenTextSplitter`

#### `split(List<Document> rawDocuments)`

- chunks an already-loaded list of documents

#### `split(List<Document>, chunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator, punctuationMarks)`

- lets you tune the token splitter directly
- this is the configurable chunking path used by the ETL controller

#### `split(Resource, chunkSize, ...)`

- combines reading and token splitting in one call

#### `advanceMarkdownSplitter(...)`

- applies the custom markdown transformer first
- then applies token splitting to the resulting sections

This is the “format-aware + token-aware” pipeline.

### `MarkdownSectionSplitter`

This is the custom transformer.

It implements Spring AI’s `DocumentTransformer`.

What it does:

- looks for markdown headings using a regex split on `\n## `
- breaks one document into markdown sections
- copies the original metadata into each section
- adds `split_type=markdown_header` to the metadata

Why it exists:

- PDFs and plain text are not always the best units for retrieval
- markdown sections often carry more meaningful structure than raw pages
- splitting by headings gives the model better context boundaries before token chunking

In other words, this is the custom “format-aware” step in the ETL pipeline.

### `ETLService`

This class orchestrates extract-transform-load.

#### `process(Resource resource)`

- reads the resource
- adds `source` metadata
- applies token splitting

#### `process(Resource resource, chunkSize, ...)`

- same as above, but with custom chunking parameters

#### `markdownProcess(Resource resource, chunkSize, ...)`

- reads the resource
- adds metadata
- applies the custom markdown transformer
- then applies token splitting

This is the class that turns a source file into embedding-ready `Document` chunks.

### `EmbeddingService`

This class handles storage into the vector store.

#### `embedDocuments(List<Document> documents)`

- uses `VectorStore.add(...)`
- lets Spring AI embed and store the documents for you

This is the preferred path for normal ingestion.

#### `embedResource(String filename)`

- loads a file from `classpath:/docs`
- runs the ETL pipeline
- stores the results in pgvector

#### `embedResource(String filename, chunkSize, ...)`

- same as above, but with custom split settings

#### `embedDirectory(String pathPattern)`

- loads all matching classpath resources
- processes each one
- stores all resulting chunks

#### `embedDirectory(String pathPattern, chunkSize, ...)`

- directory ingestion with custom chunk settings

#### `embedAndSaveDocuments(List<Document> documents)`

- simple alias for vector-store ingestion

#### `embedDocumentsLongApproach(List<Document> documents)`

- manually calls the embedding model
- serializes metadata to JSON
- inserts each row with `JdbcTemplate`
- writes the vector using `PGvector`

This method exists as an educational example of the “manual” path. The standard `VectorStore.add(...)` path is simpler and is what the app usually uses.

### `DocumentPreview`

This is a record used for display and API responses.

Fields:

- `source`
- `characterCount`
- `preview`
- `metadata`

#### `DocumentPreview.from(Document document)`

- pulls the formatted content from the document
- collapses whitespace into a single-line preview
- truncates it to 240 characters
- extracts the source from metadata

This is the shape returned by the preview endpoints and the UI-friendly APIs.

### `DocumentPreviewResponse`

This record wraps preview results in a stable response shape.

Fields:

- `totalDocuments`
- `documents`

It is used by the document reader, splitter, ETL, and vector query controllers.

## Frontend files

### `static/index.html`

The browser entry point for the chat UI.

It provides:

- a session field
- a retrieved chunk count slider
- a panel showing the streaming endpoint
- a message timeline
- a composer at the bottom of the page

### `static/app.js`

This file drives the browser chat experience.

What it does:

- creates and stores a session ID in `localStorage`
- sends messages to `POST /rag/chat/stream`
- reads the response with the Fetch API stream reader
- updates the assistant message live as chunks arrive
- auto-resizes the textarea
- keeps the UI status updated

The UI is intentionally fetch-based rather than `EventSource` because the endpoint is a streaming POST, not an SSE endpoint.

### `static/styles.css`

This is the layout and visual system for the chat UI.

It handles:

- dark theme styling
- sidebar layout
- message bubbles
- composer layout
- page scrolling
- responsive behavior on smaller screens

The page is designed to feel like a compact operational tool rather than a marketing site.

## Tests

### `SpringAILangChain4jApplicationTests`

This is the basic Spring context smoke test.

It uses the `test` profile and imports the Testcontainers pgvector configuration so the app context can start with a real PostgreSQL container.

### `PgVectorRagIntegrationTest`

This is the RAG storage/search integration test.

It verifies that:

- documents can be stored in pgvector
- similarity search returns the expected result

### `PgVectorTestConfiguration`

This is the test-only wiring used by the integration tests.

It provides:

- a `pgvector/pgvector:pg16` Testcontainers PostgreSQL instance
- a deterministic fake `EmbeddingModel`

The fake embedding model makes the test predictable and fast.

## Docker compose

`compose.yaml` runs:

- Ollama
- pgvector

The database is published on host port `5433`, which avoids conflicts with a local PostgreSQL instance on `5432`.

To bring the stack up:

```powershell
docker compose up -d
docker exec -it codebase-ollama-1 ollama pull llama3.2:1b
docker exec -it codebase-ollama-1 ollama pull nomic-embed-text
```

To shut it down:

```powershell
docker compose down
```

## How to run the app

```powershell
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080/
```

The root path forwards to the streaming chat UI.

## Endpoint reference

### Chat and RAG

- `GET /ask?question=...`
- `GET /joke?topic=...`
- `GET /rag/ask?question=...&topK=4`
- `POST /rag/chat?sessionId=...&topK=4`
- `POST /rag/chat/stream?sessionId=...&topK=4`

### Embeddings and vectors

- `GET /embed/file?filename=...`
- `GET /embed/directory?dirPath=...`
- `GET /vector/text?text=...`
- `GET /vector/query?query=...`

### Document reading and transformation

- `GET /document-reader/text`
- `GET /document-reader/pdf`
- `GET /document-reader/directory`
- `GET /document-splitter/split`
- `GET /etl`
- `GET /etl/markdown`

## Why the custom transformer matters

The custom `MarkdownSectionSplitter` is the most interesting part of the ingestion pipeline.

Plain token splitting is useful, but it does not know anything about the structure of your source material. Markdown usually has headings that define semantic sections. By splitting on headings first, you preserve meaning better before the token splitter breaks things down further.

That means:

- retrieval chunks are more likely to align with real topics
- context passed into the prompt is cleaner
- answers tend to be more relevant

That is a small change in code, but it makes a noticeable difference in RAG quality.

## Notes

- This is a playground branch, not a production-ready AI service.
- `llama3.2:1b` is chosen for speed; it is lightweight enough to feel responsive locally.
- `nomic-embed-text` is used for embeddings because it is a common local embedding model and works well with pgvector.
- The browser UI streams answers progressively so the user sees the model working instead of waiting for one big final response.
- The project still includes LangChain4j as a dependency, but the current implementation path is centered on Spring AI.

