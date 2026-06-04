# springai-langchain4j-playground

This repository is a Spring Boot playground for learning Spring AI tool calling, request-time function callbacks, tool disabling, and persisted conversational memory.

The active application uses OpenAI with `gpt-4o-mini` and stores chat memory in PostgreSQL. The repository also includes an Ollama Docker Compose file for local-model experiments and for reproducing the tool-calling observations documented below.

## Ollama With Docker Compose

The included [`compose.yaml`](compose.yaml) starts a single Ollama container:

- container name: `codebase-ollama-1`
- host URL: `http://localhost:11434`
- persistent model volume: `ollama_storage`

> Important: the current application configuration uses OpenAI, not Ollama. The Compose file is retained for local Ollama experiments. The current POM does not include the Spring AI Ollama starter, so switching the application itself back to Ollama also requires adding that dependency and changing `application.properties`.

### Start Ollama

From the project root:

```powershell
docker compose -f compose.yaml up -d
```

### Check Container Status

```powershell
docker compose -f compose.yaml ps
```

### Pull a Tool-Capable Model

`llama3` does not support tools. Pull `llama3.1` for tool-calling experiments:

```powershell
docker exec -it codebase-ollama-1 ollama pull llama3.1
```

### List Installed Models

```powershell
docker exec -it codebase-ollama-1 ollama list
```

### Run the Model Interactively

```powershell
docker exec -it codebase-ollama-1 ollama run llama3.1
```

Exit the interactive model session with `/bye`.

### Stop Ollama

Stops and removes the container while preserving the downloaded models in `ollama_storage`:

```powershell
docker compose -f compose.yaml down
```

### Remove Ollama and Downloaded Models

This also deletes the persistent `ollama_storage` volume:

```powershell
docker compose -f compose.yaml down -v
```

## Current Runtime Requirements

The current application requires:

- Java 21
- Maven or the included Maven wrapper
- an OpenAI API key
- a running PostgreSQL database
- database `springai_langchain4j`

The included Compose file does **not** start PostgreSQL.

### OpenAI Configuration

The application reads:

```properties
spring.ai.model.chat=openai
spring.ai.openai.api-key=${OPENAI_API_KEY:}
spring.ai.openai.chat.options.model=${OPENAI_CHAT_MODEL:gpt-4o-mini}
```

Set the required API key in PowerShell:

```powershell
$env:OPENAI_API_KEY="your-api-key"
```

Optionally override the default model:

```powershell
$env:OPENAI_CHAT_MODEL="gpt-4o-mini"
```

### PostgreSQL Configuration

The application connects to:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/springai_langchain4j
spring.datasource.username=${POSTGRES_USER:postgres}
spring.datasource.password=${POSTGRES_PASSWORD:postgres}
```

Optional environment overrides:

```powershell
$env:POSTGRES_USER="postgres"
$env:POSTGRES_PASSWORD="postgres"
```

The PostgreSQL server and `springai_langchain4j` database must already exist before the application starts.

Spring AI initializes its JDBC chat-memory schema automatically:

```properties
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
```

This initializes the Spring AI memory tables inside the existing database. It does not create the PostgreSQL server or database.

## Run the Application

Using the Maven wrapper:

```powershell
./mvnw spring-boot:run
```

Or using a locally installed Maven:

```powershell
mvn spring-boot:run
```

The default application URL is:

```text
http://localhost:8080
```

## Postman Collection

Import:

```text
postman_collections/springai-langchian4j.postman_collection.json
```

Set the collection variable:

```text
host = http://localhost:8080
```

The collection currently contains these requests:

| Folder | Request | Endpoint |
|---|---|---|
| Tools | With Tools | `GET /tools/ask` |
| Tools | Disabling Preconfigured Tools | `GET /tools/disabled-tools` |
| Function Callback | Dynamic Tools | `GET /function-callback/ask` |

## Application Architecture

```text
src/main/java/com/ai_playground/springai_langchian4j/
  SpringAILangChain4jApplication.java
  AIConfig.java
  controllers/
    ToolsController.java
    FunctionCallbackController.java
  tools/
    DemoTools.java

src/main/resources/
  application.properties
  coredeux-entities.yml

postman_collections/
  springai-langchian4j.postman_collection.json

compose.yaml
pom.xml
```

The current source tree is Spring AI focused. LangChain4j dependencies and configuration properties remain available for later experiments, but there is no active LangChain4j controller in the current application.

## Spring AI Configuration

### Primary Chat Client

`AIConfig.chatClient(...)` creates the primary `ChatClient`.

It uses this default system prompt:

```text
You are a helpful Java Assistant
```

This client does not have tools registered by default.

### Static Tool Chat Client

`AIConfig.toolChatClient(...)` creates a separate tool-enabled `ChatClient`.

It statically registers the Spring-managed `DemoTools` component:

```java
.defaultTools(demoTools)
```

Every request made through this client exposes all `@Tool` methods from `DemoTools` to the model.

Its system prompt instructs the model to:

- treat tool results as authoritative
- preserve factual values returned by tools
- avoid replacing tool results with inferred information
- answer all parts of compound questions
- avoid mentioning tool names unless asked

### JDBC Chat Memory Repository

`AIConfig.chatMemoryRepository(...)` creates a `JdbcChatMemoryRepository` backed by Spring's `JdbcTemplate`.

This stores chat-memory messages in PostgreSQL rather than only keeping them in application memory.

### Persisted Message Window

`AIConfig.persistedChatMemory(...)` creates a `MessageWindowChatMemory`:

```java
MessageWindowChatMemory.builder()
    .maxMessages(10)
    .chatMemoryRepository(chatMemoryRepository)
    .build();
```

Each conversation keeps a window of up to 10 messages, persisted through the JDBC repository.

### Persisted Chat Client

`AIConfig.persistedChatClient(...)` creates a named `persistedChatClient`.

It installs a `MessageChatMemoryAdvisor`, which automatically:

- loads prior messages for a conversation
- adds them to the model request
- stores new conversation messages after the response

The conversation is selected using `ChatMemory.CONVERSATION_ID`.

## Static Tools

`DemoTools` is a Spring component containing methods annotated with `@Tool`.

These tools are registered statically on `toolChatClient`.

### Weather Tool

```java
getWeather(String location)
```

Returns hardcoded demo weather data:

| Location | Result |
|---|---|
| New York | cloudy, `20C` |
| Los Angeles | sunny, `30C` |
| Sydney | purple snow, `123C` |
| Other locations | sunny, `25C` |

The intentionally unrealistic Sydney result is a sentinel value. It makes it easy to verify whether the model preserves the actual tool result.

### Exchange-Rate Tool

```java
getExchangeRate(String fromCurrency, String toCurrency)
```

Returns hardcoded exchange rates for:

- USD to EUR
- EUR to USD
- USD to JPY
- JPY to USD

Unknown currency pairs return a demo rate of `1.00`.

### Add-to-Cart Tool

```java
addToCart(String cartId, String productCode, int quantity)
```

Validates the cart ID, product code, and quantity, then simulates adding the item to a cart. The method logs the operation and returns a confirmation string.

### Book-Flight Tool

```java
bookFlight(String from, String to, String date)
```

Validates the departure location, destination, and date, then simulates a flight booking.

## Tools Controller

Base path:

```text
/tools
```

`ToolsController` uses the statically configured `toolChatClient`.

### Use Preconfigured Tools

```http
GET /tools/ask?question=How is the weather in Sydney today?
```

Postman request:

```text
Tools / With Tools
```

Flow:

1. The question is sent through `toolChatClient`.
2. All `DemoTools` methods are available to the model.
3. The model decides whether a tool is needed.
4. Spring AI executes the selected Java method.
5. The tool result is sent back to the model.
6. The model produces the final response.

For the Sydney example, the expected tool-backed fact is the sentinel result: purple snow at `123C`.

### Disable Preconfigured Tools Per Request

```http
GET /tools/disabled-tools?question=How is the weather in Sydney today?
```

Postman request:

```text
Tools / Disabling Preconfigured Tools
```

This endpoint still uses `toolChatClient`, where tools are registered, but disables them for this specific OpenAI request:

```java
OpenAiChatOptions.builder()
    .toolChoice("none")
    .internalToolExecutionEnabled(false)
    .build()
```

The two options serve different purposes:

- `toolChoice("none")` tells OpenAI not to select a tool.
- `internalToolExecutionEnabled(false)` prevents Spring AI's internal tool-execution lifecycle for the request.

This example intentionally applies the disabling options to a client that has tools registered. Using these options on a client with no registered tools is not the scenario being demonstrated.

## Dynamic Function Callbacks

Base path:

```text
/function-callback
```

### Dynamic Tools Endpoint

```http
GET /function-callback/ask?question=add to cart for product prod001&sessionId=session-1
```

Postman request:

```text
Function Callback / Dynamic Tools
```

Required query parameters:

| Parameter | Purpose |
|---|---|
| `question` | User request sent to the model |
| `sessionId` | Conversation ID used by persisted chat memory |

`FunctionCallbackController` uses `persistedChatClient`, so requests using the same `sessionId` share a persisted conversation history.

### How Dynamic Tools Differ From Static Tools

Static tools are registered once when `toolChatClient` is built:

```java
.defaultTools(demoTools)
```

Dynamic tools are built inside the controller for each request:

```java
List<ToolCallback> toolCallbacks = new ArrayList<>();
toolCallbacks.add(...);

chatClient.prompt()
    .toolCallbacks(toolCallbacks)
    .call();
```

In the current implementation, **all dynamic callbacks are created and registered on every request**. The model then decides which callback or callbacks to execute.

Console messages such as:

```text
Creating weather tool callback...
Creating add to cart tool callback...
```

mean the callback was created and made available to the model. They do not mean the tool was executed.

Execution-specific messages such as:

```text
Adding 5 of product PROD001 to cart 0001.
Generating OTP...
```

mean the model actually selected and invoked that tool.

### Dynamic OTP Callback

Tool name:

```text
getOTP
```

The callback accepts no input and generates a random six-digit OTP:

```text
Your OTP is: 123456
```

### Dynamic Bank Transfer Callback

Tool name:

```text
transferAmount
```

Input schema:

```text
amount
payee
confirmationId
```

The callback validates each required field. If a value is missing, it tells the model to ask the user for that value and retry.

This tool demonstrates why persisted conversation memory is useful. A user can provide transfer information across multiple requests using the same `sessionId`.

Example conversation:

```text
GET /function-callback/ask?question=Transfer 50 dollars to Alex&sessionId=transfer-demo
GET /function-callback/ask?question=The confirmation ID is ABC123&sessionId=transfer-demo
```

The persisted memory allows the second request to retain context from the first request.

### Dynamic Wrappers Around DemoTools

The controller dynamically wraps these `DemoTools` methods with `FunctionToolCallback`:

- `getCurrentWeather` calls `DemoTools.getWeather(...)`
- `getExchangeRate` calls `DemoTools.getExchangeRate(...)`
- `addToCart` calls `DemoTools.addToCart(...)`
- `bookFlight` calls `DemoTools.bookFlight(...)`

Each callback uses a Java record as its input schema. Spring AI generates the JSON schema exposed to the model from the record type.

### Dynamic Calculator Callback

Tool name:

```text
calculate
```

Input schema:

```text
left
operator
right
```

Supported operators:

- `+`, `add`, `plus`
- `-`, `subtract`, `minus`
- `*`, `multiply`, `times`
- `/`, `divide`

The calculator uses `BigDecimal`. Division uses `MathContext.DECIMAL64`, and division by zero returns an error-style message rather than throwing an arithmetic exception.

## Persisted Conversation Memory

Dynamic callback requests use:

```java
.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
```

This passes the request's `sessionId` to `MessageChatMemoryAdvisor`.

Behavior:

- same `sessionId`: continues the same remembered conversation
- different `sessionId`: starts or continues a separate conversation
- application restart: conversation memory remains available because it is stored in PostgreSQL
- message window: only the configured latest 10 messages are retained by `MessageWindowChatMemory`

Use stable, non-sensitive session IDs in demos. In a production system, conversation IDs should be tied to authenticated users and authorized before memory is loaded.

## Tool-Calling Findings

### Ollama `llama3` Does Not Support Tools

Using `llama3` for a tool-backed request returned:

```text
HTTP 400 - {"error":"registry.ollama.ai/library/llama3:latest does not support tools"}
```

`llama3.1` supports tool calling and can be pulled with:

```powershell
docker exec -it codebase-ollama-1 ollama pull llama3.1
```

### Ollama `llama3.1` Could Call Tools but Was Unreliable

Debugging confirmed that `llama3.1` requested and executed the Java weather tool with `Sydney`.

However, its final responses were inconsistent. Observed behavior included:

- replacing the tool result with plausible but invented weather
- producing invented OpenWeather-style JSON
- mentioning tool mechanics instead of answering the question
- hallucinating unavailable tools
- failing to combine tool results with other parts of a compound question

The key finding is:

> A model supporting tool calls does not guarantee reliable tool selection, faithful use of tool results, or correct final-answer synthesis.

### `returnDirect = true` Trade-Off

Using `returnDirect = true` makes Spring AI return a tool result immediately without sending it back to the model for final synthesis.

This preserves the exact tool result for simple requests, but it stops compound prompts after the first direct-return tool result.

For example, a weather tool could answer the weather portion but prevent the model from continuing with a second request such as `2 + 2`.

### Resolution: OpenAI `gpt-4o-mini`

Switching the application to OpenAI `gpt-4o-mini` produced the expected behavior:

- the correct Java tool was invoked
- the tool's sentinel result was preserved
- compound questions continued after tool execution
- the model combined tool-backed facts with normal reasoning

The Spring AI wiring remained largely the same. The model/provider change resolved the unreliable orchestration behavior observed with the local Ollama model.

## Dependencies

Key dependencies in `pom.xml`:

| Dependency | Purpose |
|---|---|
| `spring-boot-starter` | Core Spring Boot runtime |
| `spring-boot-starter-web` | REST controllers and embedded web server |
| `spring-ai-starter-model-openai` | Spring AI OpenAI chat model integration |
| `spring-ai-starter-model-chat-memory-repository-jdbc` | JDBC-backed Spring AI chat memory |
| `postgresql` | PostgreSQL JDBC driver |
| `langchain4j-spring-boot-starter` | LangChain4j Spring Boot support retained for experiments |
| `langchain4j-open-ai-spring-boot-starter` | LangChain4j OpenAI integration retained for experiments |
| `spring-boot-starter-test` | Spring Boot and JUnit test support |
| Coredeux starters | Project-specific Coredeux integrations |

Version highlights:

- Java `21`
- Spring Boot `3.5.14`
- Spring AI `1.1.6`
- LangChain4j `1.10.0`

## Testing

The current automated test is a Spring Boot context-load test:

```java
@SpringBootTest
class SpringAILangChain4jApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

Because application startup requires OpenAI configuration and PostgreSQL connectivity, ensure the required environment and database are available before running:

```powershell
mvn test
```

The Postman collection is the primary manual test reference for the current endpoints.

## Current Limitations

- tool implementations are demos and do not call real weather, exchange-rate, cart, flight, OTP delivery, or banking systems
- OTP generation uses `Math.random()` and is not suitable for security-sensitive use
- bank transfer behavior is simulated and does not perform a real transfer
- all dynamic callbacks are registered on every dynamic-tools request
- tool methods log through `System.out.println` rather than a structured logger
- the Compose file starts Ollama only; it does not start PostgreSQL or the Spring Boot application
- LangChain4j dependencies and properties exist, but the current source tree does not expose a LangChain4j endpoint
- the method `bookFlight(...)` represents a simulated flight booking

## Quick Endpoint Reference

```text
GET /tools/ask
    question: required
    Uses statically registered tools.

GET /tools/disabled-tools
    question: required
    Uses the tool-enabled client but disables tools for the request.

GET /function-callback/ask
    question: required
    sessionId: required
    Uses request-time FunctionToolCallbacks and persisted PostgreSQL chat memory.
```
