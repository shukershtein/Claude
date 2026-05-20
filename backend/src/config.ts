import "dotenv/config";

function required(name: string): string {
  const v = process.env[name];
  if (!v) throw new Error(`Missing required env var: ${name}`);
  return v;
}

export const config = {
  port: Number(process.env.PORT ?? 8080),
  host: process.env.HOST ?? "0.0.0.0",
  deviceToken: required("DEVICE_TOKEN"),
  databaseUrl: required("DATABASE_URL"),
  model: process.env.AGENT_MODEL ?? "claude-sonnet-4-5",
};
