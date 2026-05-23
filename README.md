# springai-langchain4j-playground

This repository is a Spring Boot proof-of-concept for comparing multiple AI providers from the same application.

The current branch, `multi-provider-strategy`, focuses on one simple question:

**How do we route the same Spring AI `ChatClient`-style usage to different model providers, and let the caller choose which provider to use at request time?**

## What this branch demonstrates

- a primary Spring AI `ChatClient` backed by OpenAI SDK
- a second `ChatClient` backed by local Ollama
- request-time selection between providers using a query parameter
- simple generative endpoints for raw questions and jokes
- the same prompt flow used against different backends

The branch is intentionally small. It is meant to show provider selection and bean wiring, not structured output, prompt templating, streaming, or UI work from the previous branch.

## Runtime model setup

The app is configured for two providers:

1. **OpenAI SDK**
   - used as the primary/default `ChatClient`
   - configured through `spring.ai.openai-sdk.*`
   - requires `OPENAI_API_KEY`

2. **Ollama**
   - used as a secondary local provider
   - configured through `spring.ai.ollama.*`
   - expects Ollama to be running locally on `http://localhost:11434`

The branch also keeps the LangChain4j Ollama settings in `application.properties`, but the active controller in this branch is using the Spring AI clients defined in `MultiModelConfig`.

## Project structure

### Configuration

[`MultiModelConfig`](src/main/java/com/ai_playground/springai_langchian4j/MultiModelConfig.java)

This configuration class defines:

- a primary `ChatClient` created from `OpenAiSdkChatModel`
- a named `ollamaChatClient`
- the underlying `OpenAiSdkChatModel`
- the underlying `OllamaChatModel`

That means the app can speak to both providers without changing the controller logic much.

### Controllers

[`GenerativeController`](src/main/java/com/ai_playground/springai_langchian4j/controllers/GenerativeController.java)

This is the main controller in the current branch. It exposes two endpoints:

- `GET /ask`
- `GET /joke`

Both endpoints accept a `model` query parameter with a default of `openai`.

If `model=ollama`, the request is routed to the named `ollamaChatClient`.
Otherwise, the primary OpenAI-backed `ChatClient` is used.

### Other source files

The project still contains the standard Spring Boot application class and a few supporting packages from earlier experiments, but the current branch is mostly about provider routing rather than prompt-engineering demos.

## Endpoint guide

### Ask a question

```text
GET /ask?question=What%20is%20Spring%20AI?&model=openai
GET /ask?question=What%20is%20Spring%20AI?&model=ollama
```

- `question` is the user prompt
- `model` chooses the backend:
  - `openai` uses the primary `ChatClient`
  - `ollama` uses the local Ollama `ChatClient`

### Ask for a joke

```text
GET /joke?topic=Donald%20Trump&model=openai
GET /joke?topic=Donald%20Trump&model=ollama
```

- `topic` is passed into a joke prompt
- the system prompt instructs the model to act like a sarcastic comedian
- `model` chooses the provider at request time

## Configuration

`src/main/resources/application.properties` contains:

- `spring.ai.openai-sdk.base-url`
- `spring.ai.openai-sdk.api-key`
- `spring.ai.openai-sdk.chat.options.model`
- `spring.ai.ollama.base-url`
- `spring.ai.ollama.chat.options.model`
- `langchain4j.ollama.chat-model.*`

Recommended environment variables:

- `OPENAI_API_KEY`
- `OPENAI_CHAT_MODEL`

If you are only testing Ollama, you still need Ollama running locally, but the OpenAI-backed bean remains part of the application wiring because it is the primary client.

## Dependencies

The POM includes:

- Spring Boot Web
- Spring AI OpenAI SDK
- Spring AI Ollama starter
- LangChain4j Spring Boot starter
- LangChain4j OpenAI support
- Coredeux starter dependencies

This branch is not trying to be a minimal dependency set. It is a playground for comparing provider integrations and keeping the same API surface available through different backends.

## Running locally

### 1. Start Ollama

If you want to use the local backend, start Ollama and ensure port `11434` is available.

### 2. Set your OpenAI key

Make sure `OPENAI_API_KEY` is available to the process that starts the app.

### 3. Run the app

```powershell
./mvnw spring-boot:run
```

or

```powershell
mvn spring-boot:run
```

### 4. Try the endpoints

Use the sample URLs above and switch `model=openai` or `model=ollama` to compare outputs.

## What this branch is for

This branch is meant to make the provider strategy visible in code:

- how beans are configured for multiple backends
- how the controller chooses the provider at runtime
- how the same prompt behaves across providers
- where the differences show up in response quality, latency, and style

It is a practical sandbox, not a polished product.

## Notes for readers

- The primary `ChatClient` is OpenAI-backed.
- The named `ollamaChatClient` is the local alternative.
- `model=ollama` is the switch that changes the backend.
- The controller uses the same prompt shape for both providers, which makes comparison easier.
- The app still carries some dependencies from earlier experiments, but the current branch is centered on multi-provider routing.
