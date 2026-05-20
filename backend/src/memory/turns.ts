import { pool } from "./db.js";

export type Role = "user" | "assistant";

export interface Turn {
  role: Role;
  content: string;
}

export async function appendTurn(convId: string, role: Role, content: string): Promise<void> {
  await pool.query(
    "INSERT INTO turns (conv_id, role, content) VALUES ($1, $2, $3)",
    [convId, role, content],
  );
}

export async function recentTurns(convId: string, limit = 20): Promise<Turn[]> {
  const { rows } = await pool.query<{ role: Role; content: string }>(
    `SELECT role, content
       FROM turns
      WHERE conv_id = $1
      ORDER BY created_at DESC
      LIMIT $2`,
    [convId, limit],
  );
  return rows.reverse();
}
