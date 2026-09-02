import { createServer, type Server } from "node:http";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { incidents, services } from "../src/data.js";
import { IncidentStore } from "../src/incident-store.js";
import { createApp } from "../src/router.js";

let server: Server;
let baseUrl: string;

beforeEach(async () => {
  server = createServer(createApp(new IncidentStore(services, incidents, () => "2025-02-12T15:00:00.000Z")));
  await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  if (!address || typeof address === "string") throw new Error("Expected a TCP server address");
  baseUrl = `http://127.0.0.1:${address.port}`;
});

afterEach(async () => {
  await new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()));
});

describe("incident HTTP API", () => {
  it("returns filtered incident summaries", async () => {
    const response = await fetch(`${baseUrl}/v1/incidents?service=billing-api&limit=10`);
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toMatchObject({ total: 1, items: [{ id: "inc-2025-041" }] });
  });

  it("rejects an unknown state filter with the standard error envelope", async () => {
    const response = await fetch(`${baseUrl}/v1/incidents?state=paused`);

    expect(response.status).toBe(400);
    await expect(response.json()).resolves.toEqual({
      error: { code: "invalid_state", message: "Unknown state: paused" }
    });
  });

  it("adds an incident update", async () => {
    const response = await fetch(`${baseUrl}/v1/incidents/inc-2025-037/updates`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ state: "monitoring", message: "Validation success rate has recovered." })
    });
    const body = await response.json();

    expect(response.status).toBe(201);
    expect(body.updates.at(-1)).toEqual({
      state: "monitoring",
      message: "Validation success rate has recovered.",
      createdAt: "2025-02-12T15:00:00.000Z"
    });
  });

  it("returns 404 for missing incidents", async () => {
    const response = await fetch(`${baseUrl}/v1/incidents/inc-missing`);
    expect(response.status).toBe(404);
  });
});
