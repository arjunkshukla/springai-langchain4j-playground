# springai-langchain4j-playground

This repository is a small Spring Boot playground for experimenting with Spring AI chat and tool calling.

The current code path is focused on Spring AI `ChatClient` with an OpenAI-backed chat model. Earlier Ollama experiments are useful because they show an important difference between "model supports tool calls" and "model reliably uses tool results."

## What this branch demonstrates

- a primary Spring AI `ChatClient` for normal chat requests
- a second Spring AI `ChatClient` configured with local Java tools
- a request-time dynamic tool callback endpoint using `FunctionToolCallback`
- plain HTTP endpoints for chat, joke generation, and tool-backed questions
- the practical difference between Ollama local tool calling and OpenAI tool calling

## Runtime model setup

The app currently uses OpenAI as the chat provider.

Configuration lives in `src/main/resources/application.properties`:

```properties
spring.application.name=springai-langchain4j

spring.ai.model.chat=openai
spring.ai.openai.api-key=${OPENAI_API_KEY:}
spring.ai.openai.chat.options.model=${OPENAI_CHAT_MODEL:gpt-4o-mini}

app.langchain4j.openai.api-key=${OPENAI_API_KEY:}
app.langchain4j.openai.model-name=${OPENAI_CHAT_MODEL:gpt-4o-mini}
```

Required environment variable:

- `OPENAI_API_KEY`

Optional environment variable:

- `OPENAI_CHAT_MODEL`

If `OPENAI_CHAT_MODEL` is not set, the app defaults to `gpt-4o-mini`.

## Project structure

```text
src/main/java/com/ai_playground/springai_langchian4j/
  SpringAILangChain4jApplication.java
  AIConfig.java
  controllers/
    GenerativeController.java
    ToolsController.java
    FunctionCallbackController.java
  tools/
    DemoTools.java

src/main/resources/
  application.properties
  coredeux-entities.yml

compose.yaml
pom.xml
```

## Application wiring

### `AIConfig`

`AIConfig` defines two Spring AI clients:

- `chatClient`
- `toolChatClient`

`chatClient` is the primary bean and is used by the regular `/ask` and `/joke` endpoints.

`toolChatClient` registers `DemoTools` via:

```java
.defaultTools(demoTools)
```

It also uses a stricter system prompt so that tool results are treated as authoritative when the model produces a final answer.

### `GenerativeController`

`GenerativeController` exposes the regular chat endpoints:

```text
GET /ask?question=...
GET /joke?topic=...
```

`/ask` sends the user question to the primary Spring AI `ChatClient`.

`/joke` adds a comedian-style system prompt and asks for a joke about the supplied topic.

### `ToolsController`

`ToolsController` exposes:

```text
GET /tools/ask?question=...
```

This endpoint uses the `toolChatClient`, so the model can request Java tool execution before producing the final response.

Example:

```text
GET /tools/ask?question=how%20is%20sydney's%20weather%20today%3F%20and%20tell%20me%20what%20is%202%20%2B%202%3F
```

Expected behavior with `gpt-4o-mini`:

```text
The current weather in Sydney is purple snow with 123C.

As for the mathematical operation, 2 + 2 equals 4.
```

### `FunctionCallbackController`

`FunctionCallbackController` exposes:

```text
GET /function-callback/ask?question=...
```

This endpoint demonstrates dynamic tools. Instead of registering a static tool bean on the `ChatClient`, it builds a request-specific list of `FunctionToolCallback` instances and passes them into:

```java
.toolCallbacks(toolCallbacks)
```

The controller currently exposes only the callbacks relevant to the incoming question:

- weather questions get a weather callback
- exchange/currency questions get an exchange-rate callback
- cart questions get an add-to-cart callback
- flight questions get a book-flight callback
- arithmetic expressions get a calculator callback

Example:

```text
GET /function-callback/ask?question=how%20is%20sydney's%20weather%20today%3F%20and%20tell%20me%20what%20is%202%20%2B%202%3F
```

That request exposes the weather and calculator callbacks for that call only.

### `DemoTools`

`DemoTools` is a local Java tool component.

It currently contains these annotated tool methods:

- `getWeather(String location)`
- `getExchangeRate(String fromCurrency, String toCurrency)`
- `addToCart(String cartId, String productCode, int quantity)`
- `bookFight(String from, String to, String date)`

These methods do not call live external APIs. They return hardcoded sample values so tool behavior is easy to test.

The Sydney weather response is intentionally unrealistic in the current experiment:

```text
The current weather in Sydney is purple snow with 123C.
```

That sentinel value makes it obvious whether the model preserved the Java tool result or invented its own answer.

## Tool-calling observations

### Finding: Ollama `llama3` does not support tools

Using Ollama with `llama3` failed immediately for tool-backed requests.

Observed error:

```text
HTTP 400 - {"error":"registry.ollama.ai/library/llama3:latest does not support tools"}
```

Resolution for that specific error:

```properties
spring.ai.ollama.chat.options.model=llama3.1
```

`llama3.1` supports tool calling, while `llama3` does not.

### Finding: `llama3.1` can call tools, but final answers were unreliable

After switching from `llama3` to `llama3.1`, the model could invoke the Java tool. Debugging confirmed that Spring AI called:

```text
getWeather("Sydney")
```

However, the final model response was inconsistent. Observed behavior included:

- replacing the tool result with plausible live-weather text
- formatting invented OpenWeather-style JSON
- saying the tool result was used instead of answering the user
- hallucinating extra tools such as a weather API tool or calculator tool
- failing compound prompts like weather plus `2 + 2`

The important lesson: tool support only means the model can request tool execution. It does not guarantee that the model will faithfully preserve the tool result in its final answer.

### Finding: `returnDirect = true` is exact but too limited for compound prompts

Spring AI tools can use `returnDirect = true`.

That makes Spring AI return the Java tool result directly after the tool executes.

This fixed simple prompts like:

```text
how is sydney's weather today?
```

But it broke compound prompts like:

```text
how is sydney's weather today? and tell me what is 2 + 2?
```

Because the response stops at the weather tool result, the model does not continue and answer the math question.

Conclusion: `returnDirect = true` is useful when the endpoint should return only the tool result. It is not a good fit for general multi-part assistant prompts.

### Resolution: switch the tool client to OpenAI

Switching the chat provider to OpenAI with `gpt-4o-mini` resolved the observed compound prompt issue.

With the current OpenAI configuration, the model:

- calls the Spring AI tool
- preserves the sentinel weather result
- continues answering the rest of the prompt

Example result:

```text
The current weather in Sydney is purple snow with a temperature of 123C.

As for the mathematical operation, 2 + 2 equals 4.
```

This is the behavior expected from a tool-using assistant: use tools for tool-backed facts, then continue reasoning over the full user request.

## Notes on Ollama

The repo still includes `compose.yaml` for running Ollama locally, and the POM still includes the Ollama starter. That makes it easy to continue local-model experiments.

If you switch back to Ollama for tool testing:

```powershell
docker compose -f compose.yaml up -d
docker exec -it codebase-ollama-1 ollama pull llama3.1
```

Then configure:

```properties
spring.ai.model.chat=ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.1
```

Use that path for experimentation, but expect weaker tool-result fidelity than OpenAI for compound prompts.

## Running locally

### 1. Set the OpenAI key

Make sure `OPENAI_API_KEY` is available to the process that runs the app.

PowerShell example:

```powershell
$env:OPENAI_API_KEY="..."
```

Optional model override:

```powershell
$env:OPENAI_CHAT_MODEL="gpt-4o-mini"
```

### 2. Run the application

```powershell
./mvnw spring-boot:run
```

or:

```powershell
mvn spring-boot:run
```

### 3. Call the endpoints

Regular chat:

```text
GET /ask?question=What%20is%20Spring%20AI?
```

Joke generation:

```text
GET /joke?topic=Spring%20Boot
```

Tool-backed chat:

```text
GET /tools/ask?question=how%20is%20sydney's%20weather%20today%3F
```

Compound tool-backed prompt:

```text
GET /tools/ask?question=how%20is%20sydney's%20weather%20today%3F%20and%20tell%20me%20what%20is%202%20%2B%202%3F
```

Dynamic tool callback example:

```text
GET /function-callback/ask?question=how%20is%20sydney's%20weather%20today%3F%20and%20tell%20me%20what%20is%202%20%2B%202%3F
```

## Dependencies

The POM includes:

- Spring Boot Web
- Spring AI OpenAI starter
- Spring AI Ollama starter
- LangChain4j Spring Boot starter
- Spring Boot test support
- Coredeux starter dependencies used by the project setup

The active application code in this branch is currently Spring AI focused. LangChain4j dependencies remain in the build, but there is no active LangChain4j controller in `src/main/java`.

## Summary

This experiment showed that the Spring AI wiring was not the problem.

The model/provider choice mattered:

- Ollama `llama3`: failed because tools are not supported.
- Ollama `llama3.1`: called tools, but final answers were unreliable for compound prompts.
- OpenAI `gpt-4o-mini`: preserved tool output and answered the full compound request.

For this branch, OpenAI is the recommended provider for tool-calling demos.
