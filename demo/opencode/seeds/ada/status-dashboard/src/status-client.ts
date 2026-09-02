import { fixtureSnapshot } from "./fixture";
import type { StatusSnapshot } from "./types";

function isStatusSnapshot(value: unknown): value is StatusSnapshot {
  if (typeof value !== "object" || value === null) return false;
  const candidate = value as Partial<StatusSnapshot>;
  return typeof candidate.generatedAt === "string" &&
    Array.isArray(candidate.services) &&
    Array.isArray(candidate.incidents);
}

export async function loadStatusSnapshot(apiUrl: string | undefined, signal?: AbortSignal): Promise<StatusSnapshot> {
  if (!apiUrl) return structuredClone(fixtureSnapshot);

  const response = await fetch(`${apiUrl.replace(/\/$/, "")}/v1/public/status`, {
    headers: { accept: "application/json" },
    signal
  });
  if (!response.ok) throw new Error(`Status API returned ${response.status}`);
  const body: unknown = await response.json();
  if (!isStatusSnapshot(body)) throw new Error("Status API returned an invalid snapshot");
  return body;
}
