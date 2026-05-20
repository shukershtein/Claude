import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { appendTurn, recentTurns } from "../memory/turns.js";
import { claudeAgent } from "../llm/claude-agent.js";
import { config } from "../config.js";

const SYSTEM_PROMPT = `You are a personal voice assistant. The user speaks to you, you reply out loud.

Style rules:
- Match the user's language. Hebrew in, Hebrew out. English in, English out.
- Keep replies short — usually one or two sentences. You're being spoken aloud, not read.
- Don't use markdown, lists, code blocks, or emoji — they don't survive text-to-speech.
- Don't read URLs or long numbers character-by-character; summarize.
- If you don't know something, say so briefly.`;

const ChatBody = z.object({
  conversationId: z.string().min(1).max(128),
  message: z.string().min(1).max(4000),
});

export function registerChatRoutes(app: FastifyInstance): void {
  app.post("/api/chat", async (req, reply) => {
    if (req.headers.authorization !== `Bearer ${config.deviceToken}`) {
      reply.code(401);
      return { error: "unauthorized" };
    }

    const parsed = ChatBody.safeParse(req.body);
    if (!parsed.success) {
      reply.code(400);
      return { error: "invalid_body", details: parsed.error.flatten() };
    }

    const { conversationId, message } = parsed.data;

    const history = await recentTurns(conversationId, 20);
    const assistantText = await claudeAgent.reply({
      systemPrompt: SYSTEM_PROMPT,
      history,
      userMessage: message,
    });

    await appendTurn(conversationId, "user", message);
    await appendTurn(conversationId, "assistant", assistantText);

    return { reply: assistantText };
  });
}
