# springai-langchain4j-playground

This repository is organized as a set of small branch-based demos.

The `main` branch is the index. It tells you which branch contains which experiment, so you can switch to the right branch before running or reading the code.

## How to use this repo

1. Read the branch summaries below.
2. Switch to the branch that matches the demo you want to inspect.
3. Open that branch's `README.md` for the branch-specific setup and endpoint guide.

Example:

```powershell
git switch base-example-with-local-llama
```

## Branch guide

| Branch | What it demonstrates |
| --- | --- |
| `base-example-with-local-llama` | A simple Spring AI playground backed by local Ollama and a single `GenerativeController` with `/ask` and `/joke`. |
| `base-example-with-openai-sdk` | Side-by-side Spring AI and LangChain4j examples using the OpenAI SDK, including LangChain4j direct model calls and AI services. |
| `multi-provider-strategy` | Provider routing at request time, letting the same endpoints choose between OpenAI and local Ollama. |
| `managinig-io-prompttemplate-converters` | Prompt templates, SpEL rendering, structured output, list/map extraction, streaming, and browser streaming demos. |
| `memory-and-context-management` | A memory-focused Spring AI and LangChain4j lab covering manual memory, in-memory chat, JDBC-backed Spring AI memory, SQL-persisted LangChain4j memory, token-window and sliding-window strategies, summarization memory, and Redis-backed chat memory. |

## Where to start

If you want:

- the simplest local model demo, switch to `base-example-with-local-llama`
- a Spring AI vs LangChain4j comparison on OpenAI, switch to `base-example-with-openai-sdk`
- runtime provider switching between OpenAI and Ollama, switch to `multi-provider-strategy`
- prompt templating, structured output, and streaming experiments, switch to `managinig-io-prompttemplate-converters`
- memory and context experiments across Spring AI, LangChain4j, Postgres, Redis, and OpenAI summarization, switch to `memory-and-context-management`

## Why the branches are split this way

Each branch keeps one idea small and readable.

That makes it easier to:

- inspect one experiment at a time
- compare approaches without unrelated code in the way
- keep the README aligned with the code in that branch
- add new demos later by creating a branch and updating this index

## Current branch

You are reading `main`, which serves as the branch directory for the rest of the project.

For detailed setup, endpoints, and runtime notes, read the `README.md` in the branch you switch to.
