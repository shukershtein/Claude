# Backend

Node.js + Fastify server. Holds the Claude Agent SDK, conversation memory, and (eventually) tool execution.

## Phase 1 surface

- `POST /api/chat` — `{ conversationId, message }` → `{ reply }`
- `GET /health`

Auth: `Authorization: Bearer <DEVICE_TOKEN>` on every request.

## Setup

```bash
cp .env.example .env
# edit .env: set DEVICE_TOKEN to a random string, keep DATABASE_URL as-is for local dev

docker compose up -d        # starts Postgres
npm install
npm run migrate             # creates the turns table
npm run dev                 # starts Fastify on :8080
```

## Claude auth (two paths)

### Max subscription (default)

On the host that runs this backend, log in once with the Claude Code CLI:

```bash
npm install -g @anthropic-ai/claude-code
claude login              # follow the OAuth flow in your browser
```

OAuth tokens get cached and the Agent SDK picks them up automatically. The backend will then use your Max subscription quota.

**On a headless VPS:** run `claude login` from your laptop first to generate the token, then copy `~/.config/anthropic/` (or wherever the CLI cached it on your laptop) up to the VPS. Re-login when the token expires.

### API key fallback

If you hit Max rate limits or want to use pay-per-token instead, set `ANTHROPIC_API_KEY` in `.env`. The SDK will prefer the API key if present.

## Smoke test

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Authorization: Bearer $(grep DEVICE_TOKEN .env | cut -d= -f2)" \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"smoke","message":"שלום, מה שלומך?"}'
```

## What's next

Phase 2 adds: pgvector for semantic recall, structured facts table with `remember_fact`/`update_fact`/`forget_fact` tools, and the wake-word handoff endpoint. None of that needs an API change to `/api/chat`.
