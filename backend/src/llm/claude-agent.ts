import { query } from "@anthropic-ai/claude-agent-sdk";
import type { LlmClient } from "./types.js";
import type { Turn } from "../memory/turns.js";
import { config } from "../config.js";

/**
 * Claude Agent SDK client.
 *
 * Auth: relies on either Max OAuth (cached by `claude login` on the host)
 * or ANTHROPIC_API_KEY env var. No tools in Phase 1 — just text in, text out.
 *
 * History is embedded directly in the prompt for Phase 1. Phase 2 will swap
 * this for semantic recall + structured facts injected via the system prompt.
 */
export const claudeAgent: LlmClient = {
  async reply({ systemPrompt, history, userMessage }) {
    const prompt = buildPrompt(history, userMessage);

    let assistantText = "";

    for await (const message of query({
      prompt,
      options: {
        model: config.model,
        systemPrompt,
        allowedTools: [],
        permissionMode: "bypassPermissions",
      },
    })) {
      if (message.type === "assistant") {
        for (const block of message.message.content) {
          if (block.type === "text") assistantText += block.text;
        }
      }
    }

    return assistantText.trim();
  },
};

function buildPrompt(history: Turn[], userMessage: string): string {
  if (history.length === 0) return userMessage;

  const lines = history.map((t) => `${t.role === "user" ? "User" : "Assistant"}: ${t.content}`);
  lines.push(`User: ${userMessage}`);
  return `Previous conversation:\n${lines.slice(0, -1).join("\n")}\n\nCurrent message:\n${lines.at(-1)}`;
}
