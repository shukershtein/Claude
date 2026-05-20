# AI Voice Agent — Project Plan

A voice-first personal AI agent for the phone. The user speaks (in Hebrew or English), the agent listens, thinks, and replies out loud. It can answer questions, control phone features, search the web, and call custom tools.

## Goals

- Voice in, voice out — push-to-talk for MVP, wake word later.
- First-class Hebrew support (STT, TTS, and LLM reasoning).
- Runs on both iOS and Android from a single codebase.
- Can do real things on the phone: calendar, reminders, messages.
- Can search the web and call custom tools / personal APIs.

## Stack

| Layer | Choice | Notes |
|---|---|---|
| App framework | **React Native (Expo)** | Cross-platform iOS + Android, easy native module access |
| LLM | **Claude Sonnet 4.6** (default), **Opus 4.7** (hard tasks) | Strong Hebrew, excellent tool use |
| STT (speech → text) | On-device, Hebrew `he-IL` | iOS `SFSpeechRecognizer`, Android `SpeechRecognizer`. Expo: `@react-native-voice/voice` |
| TTS (text → speech) | On-device, Hebrew `he-IL` | iOS `AVSpeechSynthesizer`, Android `TextToSpeech`. Expo: `expo-speech` |
| Backend | **Node.js + Fastify** or **Python + FastAPI** | Holds the Anthropic API key, runs tool execution, stores history |
| Storage | SQLite (on-device) + Postgres (backend) | Local cache for offline, server for sync |
| Auth | Single-user JWT for MVP | Expand later if shared |

### Honest tradeoff on on-device Hebrew voice

The built-in Hebrew TTS voices on iOS/Android sound robotic compared to ElevenLabs. We're starting on-device because it's free, fast, and works offline. If voice quality becomes a problem, ElevenLabs Hebrew is a drop-in upgrade in phase 4 without rewriting the app.

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                  Phone (React Native)                │
│                                                      │
│  [Mic] → on-device STT (he-IL) → text                │
│                          │                           │
│                          ▼                           │
│                    HTTPS to backend                  │
│                          │                           │
│  [Speaker] ← on-device TTS (he-IL) ← response text   │
└──────────────────────────┼───────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────┐
│                Backend (Node or Python)              │
│                                                      │
│  Auth → Conversation store → Claude API (tool use)   │
│                                       │              │
│                                       ▼              │
│              Tool execution: web search,             │
│              calendar, reminders, MCP servers,       │
│              custom APIs                             │
└──────────────────────────────────────────────────────┘
```

## Capabilities

| Capability | Implementation |
|---|---|
| General Q&A | Claude with Hebrew system prompt + conversation memory |
| Calendar (read/write events) | `expo-calendar` — works on both platforms |
| Reminders | iOS: EventKit via native module. Android: AlarmManager or Google Tasks API |
| Messages | iOS: limited to drafting via share sheet (Apple sandbox). Android: `SEND_SMS` permission allows direct send |
| Web search | Claude's built-in web search tool — no extra infrastructure |
| Custom tools | MCP servers running on backend, exposed to Claude via tool use |

## Phases

### Phase 1 — MVP voice loop (1-2 weeks)
- Expo app with a single push-to-talk button.
- Hebrew STT → text shown on screen → text sent to backend.
- Backend calls Claude (no tools yet) → response.
- Response text → Hebrew TTS → speaker.
- Goal: prove the voice loop works end-to-end in Hebrew.

### Phase 2 — Tools (1-2 weeks)
- Add web search tool.
- Add calendar tool (read + create events).
- Add reminders tool.
- Build RTL-aware transcript view in the app.
- Multi-turn conversation memory.

### Phase 3 — Custom integrations (1 week)
- MCP server scaffold on backend.
- Plug in personal APIs / smart home / work tools as MCP servers.
- Permissions model — what tools require explicit confirmation before running.

### Phase 4 — Polish
- Optional wake word ("Hey [name]") via Picovoice Porcupine.
- Background mode + lock-screen interaction.
- Optional upgrade to ElevenLabs Hebrew TTS for nicer voice.
- App icon, onboarding, settings screen.

## Open questions to decide before Phase 1

1. **Wake word vs push-to-talk?** Push-to-talk for MVP — wake word adds significant complexity and battery cost.
2. **Conversation memory scope:** session-only, or persistent across days/weeks?
3. **Distribution:** TestFlight + Play internal testing, or just sideload to personal device?
4. **Backend hosting:** Fly.io, Railway, or a small VPS?
5. **Anthropic API budget:** rough monthly cap so we pick the right default model.

## Repo layout (proposed)

```
.
├── app/                 # React Native (Expo) app
│   ├── src/
│   │   ├── screens/
│   │   ├── voice/       # STT + TTS wrappers
│   │   └── api/         # Backend client
│   └── app.json
├── backend/             # Node.js or Python server
│   ├── src/
│   │   ├── routes/
│   │   ├── claude/      # Anthropic SDK wrapper
│   │   └── tools/       # Tool implementations + MCP servers
│   └── package.json
└── PLAN.md              # This file
```

## Risks

- **Hebrew on-device TTS quality** — may push us to ElevenLabs sooner than planned.
- **iOS background mic limits** — Apple is strict about always-on listening; wake word may need to be tap-to-activate even later.
- **iOS messaging sandbox** — direct SMS send is not possible; users must confirm in the native share sheet.
- **Latency** — STT → network → Claude → TTS chain can feel slow. Streaming responses and starting TTS on first sentence will help.

## Next step

Pick answers to the open questions above, then start Phase 1.
