# springai-langchain4j-playground

This repository is a Spring Boot playground for comparing Spring AI and LangChain4j on the same basic OpenAI-backed setup.

The current branch, `base-example-with-openai-sdk`, is intentionally simple:

- one Spring AI `ChatClient` configured with the OpenAI SDK
- one LangChain4j `ChatModel` configured with OpenAI
- one LangChain4j AI service interface built on top of that model
- a small set of controller endpoints that show the two libraries side by side

The goal is to make the wiring easy to inspect and the differences easy to see.

## What this branch demonstrates

- how to create a Spring AI `ChatClient` with a default system prompt
- how to use Spring AI for plain question/answer and joke generation
- how to create a LangChain4j `ChatModel` from OpenAI settings
- how to use LangChain4j directly with `chat(...)`
- how to use LangChain4j AI services with `@SystemMessage` and `@UserMessage`
- how to keep the same app talking to the same underlying OpenAI model through two different APIs

## Runtime model setup

The app uses OpenAI as the main model provider.

Configuration lives in `src/main/resources/application.properties`:

- `spring.ai.model.chat=openai`
- `spring.ai.openai.api-key=${OPENAI_API_KEY:}`
- `spring.ai.openai.chat.options.model=${OPENAI_CHAT_MODEL:gpt-4o-mini}`
- `app.langchain4j.openai.api-key=${OPENAI_API_KEY:}`
- `app.langchain4j.openai.model-name=${OPENAI_CHAT_MODEL:gpt-4o-mini}`

Recommended environment variables:

- `OPENAI_API_KEY`
- `OPENAI_CHAT_MODEL`

If `OPENAI_CHAT_MODEL` is not set, the app defaults to `gpt-4o-mini`.

## Project structure

### Application bootstrap

[`SpringAILangChain4jApplication`](src/main/java/com/ai_playground/springai_langchian4j/SpringAILangChain4jApplication.java)

Standard Spring Boot entry point.

### Spring AI wiring

[`AIConfig`](src/main/java/com/ai_playground/springai_langchian4j/AIConfig.java)

This configuration class creates:

- a Spring AI `ChatClient` bean with a default system prompt:
  - `"You are a helpful Java Assistant"`
- a LangChain4j `ChatModel` bean named `langchain4jChatModel`
- a LangChain4j AI service bean named `langchain4jAssistant`

The Spring AI `ChatClient` is the main Spring-side integration.

The LangChain4j beans are built from the same OpenAI configuration values, so both libraries can be compared using the same model family.

### Spring AI controller

[`GenerativeController`](src/main/java/com/ai_playground/springai_langchian4j/controllers/GenerativeController.java)

This controller exposes two simple Spring AI endpoints:

- `GET /ask`
- `GET /joke`

`/ask` sends a free-form question to the primary `ChatClient`.

`/joke` adds a comedian-style system prompt and asks for a joke about a topic.

### LangChain4j controller

[`LangChain4jController`](src/main/java/com/ai_playground/springai_langchian4j/controllers/LangChain4jController.java)

This controller shows the LangChain4j side of the same idea:

- `GET /lc4j/ask`
- `GET /lc4j/joke`
- `GET /lc4j/service/joke`

`/lc4j/ask` uses the raw LangChain4j `ChatModel`.

`/lc4j/joke` uses the raw `ChatModel` with an explicit system message and user message list.

`/lc4j/service/joke` uses the LangChain4j AI service interface and is the cleanest example of the LangChain4j service abstraction in this branch.

### LangChain4j assistant interface

[`LangChain4jAssistant`](src/main/java/com/ai_playground/springai_langchian4j/LangChain4jAssistant.java)

This interface defines the LangChain4j AI service method:

- `tellJoke(String topic)`

It is annotated with:

- `@SystemMessage("You are a comedian. Be sarcastic and funny.")`
- `@UserMessage("Tell me a joke about {{topic}}")`

That gives you the same joke behavior as the controller, but through LangChain4j's service abstraction.

## Endpoint guide

### Spring AI examples

```text
GET /ask?question=What%20is%20Spring%20AI?
GET /joke?topic=Donald%20Trump
```

### LangChain4j examples

```text
GET /lc4j/ask?question=What%20is%20LangChain4j?
GET /lc4j/joke?topic=Donald%20Trump
GET /lc4j/service/joke?topic=Donald%20Trump
```

## Dependencies

The POM includes:

- Spring Boot Web
- Spring AI OpenAI SDK
- LangChain4j Spring Boot starter
- LangChain4j OpenAI support
- Spring Boot test support
- Coredeux starter dependencies used by the project setup

## How the two stacks differ in this branch

This branch is useful because it keeps the same kind of requests on both sides:

- Spring AI:
  - `ChatClient`
  - fluent prompt building
  - `.call()` for synchronous response retrieval
- LangChain4j:
  - direct `ChatModel.chat(...)`
  - message lists for system/user roles
  - AI service interface via `AiServices.create(...)`

You can use the same topic or question and compare how the two libraries feel from the controller layer.

## Running locally

### 1. Set the OpenAI key

Make sure `OPENAI_API_KEY` is available to the process that runs the app.

### 2. Run the application

```powershell
./mvnw spring-boot:run
```

or

```powershell
mvn spring-boot:run
```

### 3. Call the endpoints

Use the sample URLs above and compare the responses from Spring AI and LangChain4j.

## Notes for readers

- This branch is deliberately small and focused.
- The interesting part is not the prompt itself, but the way the same prompt is wired through different abstractions.
- The Spring AI and LangChain4j pieces are both using OpenAI in this branch, so differences in behavior are easier to attribute to the library layer rather than the provider.

## Previous experiments

Earlier branches in this repo explored prompt templates, streaming, Ollama, structured output, and other response-mapping experiments. Those are not the focus of the current branch.

If you are reading this branch from scratch, start with:

1. `AIConfig`
2. `GenerativeController`
3. `LangChain4jController`
4. `LangChain4jAssistant`

That gives you the whole story of the branch in a few files.
