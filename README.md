# springai-langchain4j-playground

This project demonstrates using Spring + LangChain-style integrations. The section below explains how to run the Ollama service locally using Docker Compose.

## Run Ollama locally (Docker)

Prerequisites
- Docker (Desktop/daemon) installed and running on your machine.
- Port 11434 must be available on the host.

Files
- compose.yaml — Docker Compose file for the ollama service. This repository sets a fixed container name so you can address it directly.

Validate the Compose (compose.yaml) file

```powershell
# From root of the project execute the below command to validate the compose.yaml file.

docker compose -f compose.yaml config
```

Start the service in the background

```powershell
# From root of the project execute the below command to start the service in the background (detached mode).
docker compose -f compose.yaml up -d
```

Check running containers

```powershell
# show Compose/ Docker ps output
docker compose -f compose.yaml ps
# or
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}"
```

Run the ollama command inside the container

This repository's compose.yaml sets a fixed container name `codebase-ollama-1`. Use that name when running commands inside the container:

```bash
# pull (if needed) and run the Llama 3 model inside the container
docker exec -it codebase-ollama-1 ollama run llama3
```

Running docker exec -it codebase-ollama-1 ollama run llama3 will pull (if not already present) and run the Llama 3 model inside that container — Llama 3 provides a strong balance of speed and intelligence. Ensure the container is running before executing the command.

Model note
- Llama 3 is a good default for development and evaluation due to its balance of speed and intelligence.

Follow logs

```powershell
docker compose -f compose.yaml logs -f ollama
```

Stop and remove containers

```powershell
docker compose -f compose.yaml down
```

Notes
- Use `docker compose` (Compose V2) if available; fall back to `docker-compose` if necessary.
- If you prefer a different fixed container name, edit compose.yaml and set `container_name: <your-name>` under the service.
