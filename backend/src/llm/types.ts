import type { Turn } from "../memory/turns.js";

export interface LlmClient {
  reply(args: { systemPrompt: string; history: Turn[]; userMessage: string }): Promise<string>;
}
