import Fastify from "fastify";
import cors from "@fastify/cors";
import { config } from "./config.js";
import { registerChatRoutes } from "./routes/chat.js";

const app = Fastify({ logger: true });

await app.register(cors, { origin: true });

app.get("/health", async () => ({ ok: true }));

registerChatRoutes(app);

try {
  await app.listen({ port: config.port, host: config.host });
} catch (err) {
  app.log.error(err);
  process.exit(1);
}
