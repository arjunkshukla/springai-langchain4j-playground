# springai-langchain4j-playground

This branch is a small Spring Boot playground for talking to a **local Ollama model** from a web controller using **Spring AI**.

The code is intentionally minimal:

- one Spring Boot application
- one `ChatClient` bean with a default system prompt
- one controller with two basic chat endpoints
- one Docker Compose file for bringing up Ollama and pgvector locally

It is meant as a lightweight starting point for experimenting with prompt styles, local model configuration, and basic AI-backed HTTP endpoints.

## What this branch contains

The current branch is centered around a local Llama model served by Ollama.

### Application wiring

- `SpringAILangChain4jApplication` bootstraps the app.
- `AIConfig` defines a Spring-managed `ChatClient` bean.
- `GenerativeController` exposes the chat endpoints.

The `ChatClient` is configured with a default system prompt:

```text
You are a helpful Java Assistant
```

That means every request starts with the same baseline behavior unless the controller overrides it for a specific endpoint.

### Current runtime behavior

The application is configured in `src/main/resources/application.properties` to use **Ollama** for Spring AI:

- chat model provider: `ollama`
- embedding provider: `ollama`
- audio/image/moderation providers: disabled
- Ollama base URL: `http://localhost:11434`
- chat model: `llama3`
- embedding model: `nomic-embed-text`
- PostgreSQL / pgvector: `jdbc:postgresql://localhost:5433/rag`

The `langchain4j` dependency is still present in the build, but the current branch code path is focused on the Spring AI `ChatClient` controller flow.

If you want to inspect pgvector from pgAdmin or another local Postgres client, connect to:

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
  controllers/GenerativeController.java

src/main/resources/
  application.properties
  coredeux-entities.yml

compose.yaml
```

## Dependencies

This branch uses:

- Spring Boot 3.5.14
- Spring AI 1.1.6
- LangChain4j 0.35.0
- Spring Web
- Spring Boot Test
- Coredeux Spring Boot starters

The build is set up for Java 21.

## Configuration

The application properties are already set up for local Ollama and local pgvector:

```properties
spring.ai.model.chat=ollama
spring.ai.model.embedding=ollama
spring.ai.model.audio.speech=none
spring.ai.model.audio.transcription=none
spring.ai.model.image=none
spring.ai.model.moderation=none
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3
spring.ai.ollama.embedding.options.model=nomic-embed-text
spring.ai.ollama.embedding.model=nomic-embed-text

spring.datasource.url=jdbc:postgresql://localhost:5433/rag
spring.datasource.username=test
spring.datasource.password=test
spring.datasource.driver-class-name=org.postgresql.Driver

langchain4j.ollama.chat-model.base-url=http://localhost:11434
langchain4j.ollama.chat-model.model-name=llama3
```

So the app expects Ollama to already be reachable on port `11434`.

## Running the containers locally

`compose.yaml` now brings up two containers:

- Ollama for local chat and embeddings
- pgvector for vector storage during tests and local runs

### Start the stack

```powershell
docker compose up -d
```

### Check that it is running

```powershell
docker compose ps
```

### Pull the Ollama models

```powershell
docker exec -it codebase-ollama-1 ollama pull llama3
docker exec -it codebase-ollama-1 ollama pull nomic-embed-text
```

### Stop the stack

```powershell
docker compose down
```

## Running the app

Run the Spring Boot app from the project root:

```powershell
mvn spring-boot:run
```

This is the easiest local flow when Docker is only providing Ollama and pgvector.

## HTTP endpoints

The controller exposes two simple GET endpoints:

### `GET /ask?question=...`

Sends a free-form question directly to the model.

Example:

```text
/ask?question=What is the difference between an interface and an abstract class?
```

### `GET /joke?topic=...`

Uses a small system prompt that asks the model to be sarcastic and funny, then generates a joke about the supplied topic.

Example:

```text
/joke?topic=Spring Boot
```

## How the app works

The important part of the current branch is the prompt flow:

1. Spring injects a `ChatClient` bean configured in `AIConfig`.
2. `GenerativeController` receives the user request.
3. The controller builds a prompt using Spring AI's fluent `ChatClient` API.
4. The prompt is sent to the local Ollama model.
5. The response content is returned as a plain string.

The `call()` path is synchronous, so the request thread waits until the model returns a response.

## Notes

- This is a playground branch, not a production-ready AI service.
- The local model is `llama3`, so output quality and latency depend on the machine running Ollama.
- The `langchain4j` dependency is present in the build, but the current code in this branch does not expose a LangChain4j controller yet.
- `coredeux-entities.yml` is present as part of the project resources, but the current branch focus is the Spring AI + local Ollama chat flow.
- `SpringAILangChain4jApplicationTests` uses the `test` profile and Testcontainers so the Spring context can start without depending on your local PostgreSQL container.
- The `PgVectorRagIntegrationTest` uses Testcontainers to spin up pgvector and verify vector storage.

## Useful files

- [`pom.xml`](pom.xml)
- [`compose.yaml`](compose.yaml)
- [`src/main/resources/application.properties`](src/main/resources/application.properties)
- [`src/main/java/com/ai_playground/springai_langchian4j/AIConfig.java`](src/main/java/com/ai_playground/springai_langchian4j/AIConfig.java)
- [`src/main/java/com/ai_playground/springai_langchian4j/controllers/GenerativeController.java`](src/main/java/com/ai_playground/springai_langchian4j/controllers/GenerativeController.java)
