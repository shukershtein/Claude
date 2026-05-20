# AI Voice Agent

A personal Hebrew/English voice agent for Android, powered by Claude.

See [PLAN.md](./PLAN.md) for the full project plan.

## Layout

- [`android/`](./android) — native Android app (Kotlin + Jetpack Compose)
- [`backend/`](./backend) — Node.js + Fastify server (Claude Agent SDK + Postgres)

## Phase 1 status

Push-to-talk voice loop end-to-end in Hebrew. No tools, no wake word — those land in Phase 2+.

## Getting started

1. Start the backend: see [`backend/README.md`](./backend/README.md)
2. Open the Android project in Android Studio: see [`android/README.md`](./android/README.md)
3. Set `BACKEND_URL` in the app to point at your backend (default `http://10.0.2.2:8080` for the emulator).
