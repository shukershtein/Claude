# AI Voice Agent — Project Plan

A voice-first personal AI agent for the phone. The user speaks (in Hebrew or English), the agent listens, thinks, and replies out loud. It can answer questions, control phone features, search the web, and call custom tools.

## Goals

- Voice in, voice out — always-on **wake word** activation.
- First-class Hebrew support (STT, TTS, and LLM reasoning).
- Runs on both iOS and Android from a single codebase.
- Can do real things on the phone: calendar, reminders, messages.
- Can search the web and call custom tools / personal APIs.
- **Persistent memory + learning** — remembers everything across sessions and adapts to the user over time.
- **Single user, single device** — only the owner uses it, only on their phone.

## Stack

| Layer | Choice | Notes |
|---|---|---|
| App framework | **React Native (Expo)** | Cross-platform iOS + Android, easy native module access |
| LLM | **Claude Sonnet 4.6** (default), **Opus 4.7** (hard tasks) | Strong Hebrew, excellent tool use |
| LLM auth | **Claude Max subscription via Claude Agent SDK** | OAuth login on the VPS, no per-token billing — see tradeoffs below |
| STT (speech → text) | On-device, Hebrew `he-IL` | iOS `SFSpeechRecognizer`, Android `SpeechRecognizer`. Expo: `@react-native-voice/voice` |
| TTS (text → speech) | On-device, Hebrew `he-IL` | iOS `AVSpeechSynthesizer`, Android `TextToSpeech`. Expo: `expo-speech` |
| Wake word | **Picovoice Porcupine** | On-device, free for personal use, trainable to any Hebrew or English phrase. Wake phrase: **"hey agent"** |
| Backend | **Node.js + Fastify** on a **Hetzner VPS** (~$5/mo) | Runs the Agent SDK, holds OAuth tokens, runs tool execution, stores memory |
| Storage | SQLite (on-device cache) + Postgres (backend, source of truth) | Persistent memory across sessions |
| Long-term memory | Postgres + pgvector for semantic recall, plus a structured "facts" table | See Memory & Learning section |
| Auth | Single hardcoded device token | One user, one device — no login flow needed |

### Honest tradeoff on on-device Hebrew voice

The built-in Hebrew TTS voices on iOS/Android sound robotic compared to ElevenLabs. We're starting on-device because it's free, fast, and works offline. If voice quality becomes a problem, ElevenLabs Hebrew is a drop-in upgrade in phase 4 without rewriting the app.

### Honest tradeoff on Claude Max as the backend LLM

Claude Max is a **consumer subscription** designed for interactive use (claude.ai + Claude Code). The Claude Agent SDK can authenticate via Max OAuth, so technically the backend can use your subscription instead of paying per-token API fees. Real concerns to know about:

- **Rate limits.** Max subscriptions have usage caps measured in messages over rolling windows. A chatty voice agent can burn through them fast — especially if Phase 4's nightly summarization jobs are aggressive. If you hit the limit, the agent goes silent until the window resets.
- **Terms of service.** Subscriptions are for personal use. A personal voice agent that only you use *is* personal use, so this should be fine — but it's not the intended path, and Anthropic could change policy.
- **Auth refresh.** OAuth tokens need refreshing. The Agent SDK handles this when run interactively, but a headless VPS process needs a re-login flow if the token expires or the session is invalidated.
- **No fallback during outages.** With an API key you can route around issues; with a subscription you're tied to the consumer plane.

**Plan:** start with Max subscription via Agent SDK. Build the backend so the LLM call is a single abstracted function — swapping to a pay-as-you-go API key later is a one-file change. If we hit rate limits in real use, that's the signal to switch.

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
| Memory & learning | Hybrid system — see next section |

## Memory & Learning

The agent should feel like it actually knows you. Three layers:

1. **Raw transcript log** — every conversation stored verbatim in Postgres. Cheap, complete record.
2. **Semantic memory (pgvector)** — each turn embedded and indexed. On every new request, retrieve the top-K most relevant past turns and inject into Claude's context. This is how "remembering" works at scale without blowing the context window.
3. **Structured facts table** — a curated set of high-confidence facts about the user (name, family, preferences, recurring schedule, work, contacts). Claude can read and **write** to this table via dedicated tools:
   - `remember_fact(key, value, source_turn_id)`
   - `update_fact(key, new_value)`
   - `forget_fact(key)`
   Whenever you tell it something stable ("my wife's name is X", "I prefer meetings after 10am"), it writes a fact. Every new conversation loads the full facts table into the system prompt — small, dense, always-on context.

**"Learning" in practice:**
- Claude proposes fact writes on its own when it detects stable preferences in conversation.
- Corrections ("no, I actually prefer X") trigger `update_fact`.
- A nightly background job summarizes the day's conversations and proposes new facts to add.
- Optionally, a weekly review where the agent reads back what it learned and asks you to confirm.

## Phases

### Phase 1 — MVP voice loop (1-2 weeks)
- Expo app with push-to-talk button (wake word comes in Phase 2).
- Hebrew STT → text shown on screen → text sent to backend.
- Backend calls Claude (no tools yet) → response.
- Response text → Hebrew TTS → speaker.
- Basic conversation memory (last N turns in context).
- Goal: prove the voice loop works end-to-end in Hebrew.

### Phase 2 — Wake word + persistent memory (1-2 weeks)
- Integrate Picovoice Porcupine, train a custom Hebrew/English wake phrase.
- Background listening service (foreground service on Android, audio session on iOS).
- Postgres + pgvector setup on backend.
- Semantic recall pipeline — embed every turn, retrieve top-K on new requests.
- Structured facts table + `remember_fact`/`update_fact`/`forget_fact` tools.

### Phase 3 — Tools (1-2 weeks)
- Web search tool.
- Calendar tool (read + create events) via `expo-calendar`.
- Reminders tool.
- RTL-aware transcript view in the app.
- Permissions model — which tools auto-run vs require voice confirmation.

### Phase 4 — Custom integrations + learning loop (1 week)
- MCP server scaffold on backend.
- Plug in personal APIs / smart home / work tools as MCP servers.
- Nightly background job: summarize conversations, propose new facts.
- Weekly review flow — agent reads back what it learned.

### Phase 5 — Polish
- Optional ElevenLabs Hebrew TTS upgrade if on-device voice quality is too robotic.
- Lock-screen interaction.
- App icon, settings screen.

## Decisions locked in

| Question | Answer |
|---|---|
| Activation | **Wake word** (Picovoice Porcupine), phrase: **"hey agent"** |
| Memory | **Persistent + learning** — full transcript log, semantic recall, structured facts |
| Users | **Just me** — no multi-user, no login |
| Distribution | **Sideload to my own phone only** — no app store, no TestFlight beta |
| Hosting | **VPS** (Hetzner CX22 or similar, ~$5/mo) |
| LLM access | **Claude Max subscription via Agent SDK OAuth** — abstracted so we can switch to API key if rate limits bite |

## Still to decide

1. **iOS sideload strategy** — free Apple Developer account (7-day re-signing required) vs $99/year paid account (1-year builds). Android sideload is free either way.

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
- **iOS background mic limits** — Apple restricts always-on listening. Porcupine works in the background but only while the app holds an active audio session; we may need a persistent notification to keep the session alive, or a "tap to wake the wake word" pattern.
- **iOS messaging sandbox** — direct SMS send is not possible; the agent can only draft via the share sheet.
- **Latency** — STT → network → Claude → TTS chain can feel slow. Mitigations: stream Claude's response and start TTS on the first sentence; cache embeddings; keep system prompt small.
- **Memory cost growth** — embedding every turn forever costs storage + embedding API calls. Not a problem for a single user, but worth noting.
- **Sideload friction on iOS** — free dev account requires re-signing every 7 days. Annoying but workable.
- **Max subscription rate limits** — see tradeoff section. The mitigation is built into the architecture: LLM calls go through one abstraction so we can swap to API key in an afternoon if needed.
- **OAuth token expiry on headless VPS** — needs a re-login flow. Worth scripting early so it's not a 3am panic.

## Next step

Lock the three remaining decisions (wake phrase, hosting, budget), then start Phase 1.
