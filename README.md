# springai-langchain4j-playground

This repository is a Spring Boot playground for experimenting with Spring AI, LangChain4j, prompt templates, streaming responses, and structured output extraction from an LLM.

The current branch focuses on how different Spring AI response-mapping approaches behave in practice:

- plain text chat responses
- prompt templates from strings and resource files
- custom SpEL-based prompt rendering
- structured output with `BeanOutputConverter`
- native structured output via advisors
- `List<String>` and `Map<String, Object>` extraction
- SSE streaming from `ChatClient.stream()`
- a simple browser page that streams responses into the UI

## What the app uses

- Java 21
- Spring Boot 3.5.14
- Spring AI 1.1.6
- LangChain4j 0.35.0
- Local Ollama at `http://localhost:11434`

The app is configured to use Ollama for chat and embeddings in `src/main/resources/application.properties`.

## Project layout

### Controllers

- `GenerativeController`
  - `/ask`
  - `/joke`
- `PromptController`
  - `/prompt/inline`
  - `/prompt/read-from-template-file`
  - `/prompt/using-spel`
- `ResponseMappingController`
  - `/response-mapping/bean-output-converter`
  - `/response-mapping/bean-output-converter-v2`
  - `/response-mapping/native-structured-output`
  - `/response-mapping/list-output-converter`
  - `/response-mapping/list-output-converter-v2`
  - `/response-mapping/native-structured-output-list`
  - `/response-mapping/map-output-converter`
  - `/response-mapping/map-output-converter-v2`
  - `/response-mapping/native-structured-output-map`
- `StreamController`
  - `/stream/joke`

### Supporting code

- `AIConfig`
  - creates the shared Spring AI `ChatClient`
- `SpelPromptTemplateRenderer`
  - custom `TemplateRenderer` that evaluates `#{...}` expressions with SpEL
- DTOs
  - `MeetingSummary`
  - `ActionItem`
  - `Priority`

### Resources

- `src/main/resources/meeting-transcript.txt`
  - transcript used by the structured-output demos
- `src/main/resources/prompt/prompt_template.st`
  - StringTemplate prompt file
- `src/main/resources/prompt/user-analysis.st`
  - SpEL-driven prompt file
- `src/main/resources/static/index.html`
  - minimal browser page for streaming demo

## What each demo shows

### Generative text

`GenerativeController` is the simplest entry point:

- `/ask?question=...` sends raw user text to the model
- `/joke?topic=...` adds a system prompt and asks for a joke

These endpoints are the baseline for the rest of the playground.

### Prompt templates

`PromptController` demonstrates three prompt styles:

1. Inline template string
2. Template loaded from `prompt_template.st`
3. Custom SpEL rendering via `user-analysis.st`

The SpEL example uses a custom renderer so `#{...}` expressions are evaluated before the prompt goes to the model.

### Structured output experiments

`ResponseMappingController` is the main branch experiment area. It compares:

- `BeanOutputConverter` with explicit format injection
- `entity(...)` convenience methods that use Spring AI's structured-output plumbing internally
- native structured output via `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT`
- list extraction
- map extraction

The comments in this controller document the behavior we observed:

- prompt-and-parse is easy to use but not a hard guarantee
- the model can still omit fields or return incomplete JSON
- native structured output is the more reliable path for object-shaped DTOs
- open-ended `List<String>` and `Map<String, Object>` requests are more brittle than a closed DTO

### Streaming

`StreamController` uses `chatClient.prompt().stream().content()` and exposes Server-Sent Events:

- `/stream/joke?topic=...`

The page at `/index.html` uses `EventSource` to stream the chunks live into the browser.

## Running Ollama locally

Prerequisites:

- Docker Desktop or Docker Engine
- Port `11434` available

Bring up the local Ollama container:

```powershell
docker compose -f compose.yaml up -d
```

Check the container:

```powershell
docker compose -f compose.yaml ps
```

Pull and run a model inside the container:

```bash
docker exec -it codebase-ollama-1 ollama run llama3
```

Stop the container:

```powershell
docker compose -f compose.yaml down
```

## Run the application

From the project root:

```powershell
./mvnw spring-boot:run
```

If you prefer Maven directly:

```powershell
mvn spring-boot:run
```

The app will use the Ollama settings defined in `application.properties`.

## Try it in the browser

Once the app is running, open:

```text
http://localhost:8080/
```

The page lets you enter a topic and stream the response from `/stream/joke` into the UI.

## Endpoint guide

### Plain chat

- `GET /ask?question=What is Spring AI?`
- `GET /joke?topic=Ollama`

### Prompt templates

- `GET /prompt/inline?topic=...`
- `GET /prompt/read-from-template-file?topic=...&length=50`
- `GET /prompt/using-spel?name=Alice&isPremium=true&loginCount=120`

### Structured output

- `GET /response-mapping/bean-output-converter`
- `GET /response-mapping/bean-output-converter-v2`
- `GET /response-mapping/native-structured-output`
- `GET /response-mapping/list-output-converter`
- `GET /response-mapping/list-output-converter-v2`
- `GET /response-mapping/native-structured-output-list`
- `GET /response-mapping/map-output-converter`
- `GET /response-mapping/map-output-converter-v2`
- `GET /response-mapping/native-structured-output-map`

### Streaming

- `GET /stream/joke?topic=Donald%20Trump`

## Notes on structured output

This branch intentionally keeps a few different response-mapping styles side by side so the differences are visible:

- `BeanOutputConverter`
  - prompt contains schema instructions
  - response is parsed afterward
  - easy to understand, but still only prompt-and-parse
- `entity(...)`
  - shorter controller code
  - Spring AI injects the format internally
  - still not a hard validator
- `native structured output`
  - best reliability for DTO-shaped outputs
  - uses the model's structured-output capability
  - better when the target shape is well defined

The code comments in `ResponseMappingController` capture the issues we ran into, including:

- enum values drifting outside the expected set
- incomplete JSON responses
- empty list/map results for open-ended `List<String>` and `Map<String, Object>` targets

## A few practical details

- The app currently uses Ollama, not OpenAI, for the main runtime configuration.
- Some of the source comments intentionally document the experiments and failures we observed, so the repository doubles as a learning log.
- The `org/` source files that may appear in the working tree are extracted inspection artifacts from Spring AI jars and should not be committed.

## If you are reading the code for the first time

The quickest path through the project is:

1. Start with `GenerativeController`
2. Look at `PromptController`
3. Read through `ResponseMappingController`
4. Check `StreamController`
5. Open `static/index.html` to see the browser streaming demo

That gives you the full arc of the branch: simple text generation, templating, structured output, and streaming.
