# RAG Document Q&A

A full-stack Retrieval-Augmented Generation (RAG) application: upload documents
(PDF or text), then ask natural-language questions answered strictly from
their content. Built with Spring Boot + pgvector on the backend and a React
frontend, with the RAG pipeline also exposed as MCP (Model Context Protocol)
tools so any MCP-compatible AI client can call it directly.

<!-- Add a screenshot of the running UI here before sharing this repo, e.g.:
![App screenshot](docs/screenshot.png) -->

## Project layout

```
rag-doc-qa/
├── src/                  Spring Boot backend (Java 17)
├── frontend/             React + Vite frontend
├── docker-compose.yml    Postgres (pgvector) + Ollama + backend, one command
├── Dockerfile            Backend container build
└── schema.sql            Database schema, auto-applied on startup
```

## Why this exists

Built to get hands-on, production-shaped experience with RAG rather than a
notebook demo — this runs as a real containerized service with proper error
handling, batching, guardrails, tests, an MCP interface, and a usable UI,
using the same Spring Boot patterns used in day-to-day backend work.

## Quick start

```bash
git clone <your-repo-url>
cd rag-doc-qa
docker compose up --build      # backend + Postgres + Ollama, one command
```
Then, in a separate terminal, for the UI:
```bash
cd frontend
npm install
npm run dev                    # opens on http://localhost:5173
```
See "Running locally" below for details on what each command does.

## Architecture

```
                 ┌─────────────────┐
  PDF/text  ───▶ │  IngestController │
                 └────────┬─────────┘
                          │
                 ┌────────▼─────────┐      1. Extract text (PDFBox)
                 │ IngestionService  │      2. Split into overlapping chunks
                 └────────┬─────────┘      3. Batch-embed via Ollama
                          │
                 ┌────────▼─────────┐
                 │  EmbeddingService │──▶ Embeddings API (Ollama)
                 └────────┬─────────┘
                          │
                 ┌────────▼─────────┐
                 │  ChunkRepository  │──▶ Postgres + pgvector (cosine search)
                 └───────────────────┘

  Question  ───▶ ┌──────────────────┐
                 │  QueryController  │
                 └────────┬─────────┘
                          │
                 ┌────────▼─────────┐      1. Embed the question
                 │    RagService     │      2. Retrieve top-K similar chunks
                 └────────┬─────────┘      3. Ask LLM using only that context
                          │
                 ┌────────▼─────────┐
                 │    LlmService     │──▶ Chat API (Ollama)
                 └───────────────────┘
```

## Key design decisions

- **pgvector over a dedicated vector DB** — keeps the stack to one database
  (Postgres), which is what most teams already run in production, instead of
  introducing a new piece of infrastructure just for embeddings.
- **Word-based chunking with overlap** — a fixed word count is simple and
  predictable to reason about; the overlap prevents a fact from being split
  across two chunks and silently becoming unretrievable.
- **Batch embedding calls during ingestion** — one HTTP call for N chunks
  instead of N calls, since embedding latency and API cost both scale with
  request count, not just token count.
- **Explicit "I don't have enough information" guardrail** — the system
  prompt forces the model to refuse rather than hallucinate when retrieved
  context doesn't actually answer the question.
- **JDBC + raw SQL for the vector column** — Spring Data JPA doesn't have
  first-class `vector` type support yet, so `JdbcTemplate` with pgvector's
  `<=>` cosine-distance operator keeps the query explicit and fast.

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker + Docker Compose
- ~6 GB free RAM and ~3 GB disk (for the local Ollama models). No API key needed.

## Running locally (100% free, no API key)

```bash
docker compose up --build
```

First run downloads two models (`nomic-embed-text` for embeddings, `llama3.2:3b`
for answers, roughly 2.5 GB total), so give it a few minutes. This starts:
- `postgres`: Postgres 16 with pgvector, schema auto-applied (see `schema.sql`)
- `ollama`: local LLM server; `ollama-init` pulls the models once
- `app`: the Spring Boot service on `http://localhost:8080`

Answers come from a small local model, so quality is lower than GPT-4-class
models; the pipeline is identical. To use a hosted provider instead, set
`LLM_BASE_URL`, `LLM_API_KEY`, `CHAT_MODEL`, `EMBEDDING_MODEL` and
`EMBEDDING_DIMENSIONS` (and change `VECTOR(768)` in `schema.sql` to match).

### Frontend

```bash
cd frontend
npm install
npm run dev
```
Opens on `http://localhost:5173` and proxies `/api` calls to the backend on
port 8080 (see `frontend/vite.config.js`), so no CORS setup is needed for
local development. Needs Node 18+ (Node 16 works with the pinned Vite 4
version in `frontend/package.json`).

## API

### Ingest a document

```bash
curl -X POST http://localhost:8080/api/ingest \
  -F "file=@/path/to/document.pdf"
```

Response:
```json
{
  "documentId": "a1b2c3d4-...",
  "documentName": "document.pdf",
  "chunkCount": 42
}
```

### Ask a question

```bash
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the notice period mentioned in this document?"}'
```

Response:
```json
{
  "answer": "The notice period is 60 days, as stated in Snippet 2.",
  "sources": [
    { "documentName": "document.pdf", "chunkIndex": 3, "snippet": "...", "similarityScore": 0.87 }
  ],
  "retrievalTimeMs": 42,
  "totalTimeMs": 890
}
```

Optionally scope the search to one document by passing its `documentId`:
```json
{ "question": "...", "documentId": "a1b2c3d4-..." }
```

## Running tests

```bash
mvn test
```

Covers the chunking logic (boundary conditions, overlap correctness, empty
input, whitespace normalization) — the one part of the pipeline that has no
external API dependency and can be tested deterministically.

## MCP (Model Context Protocol) support

This service exposes its RAG pipeline as MCP tools, using Spring AI's MCP
server starter (`spring-ai-starter-mcp-server-webmvc`), so any MCP-compatible
client (Claude Desktop, the MCP Inspector, a custom agent) can call it
directly instead of going through the REST API.

**Tools exposed:**
- `askDocuments(question, documentId?)` - returns just the answer text
- `askDocumentsWithSources(question, documentId?)` - returns the answer plus
  which chunks and similarity scores it was grounded on

Both wrap the same `RagService` the REST controllers use, so there is one
source of truth for the retrieval logic - the MCP layer only adapts it to
the protocol's tool-call shape.

**Endpoints** (same host/port as the REST API, no separate server):
- `GET /sse` - opens the MCP event stream
- `POST /mcp/message` - where the client sends tool calls

**Testing it with the MCP Inspector** (no code needed):
```bash
npx @modelcontextprotocol/inspector
```
Open the URL it prints, choose SSE transport, and connect to
`http://localhost:8080/sse`. The Inspector lists both tools and lets you
call them with a question and see the JSON response live.

**Connecting from Claude Desktop:** add this to your MCP client config
(exact file location depends on the client) and restart it:
```json
{
  "mcpServers": {
    "rag-doc-qa": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

## Other possible extensions

- Swap the LLM/embedding provider to Anthropic's Claude API.
- Add re-ranking of retrieved chunks before sending them to the LLM.
- Stream the LLM response instead of waiting for the full completion.
- Add an MCP tool for ingestion, not just querying, so a client can upload
  documents through MCP as well as ask about them.

## Tech stack

Java 17, Spring Boot 3, Spring Web, Spring JDBC, Spring AI (MCP server),
PostgreSQL + pgvector, Apache PDFBox, JUnit 5, Docker, Docker Compose,
React, Vite.
