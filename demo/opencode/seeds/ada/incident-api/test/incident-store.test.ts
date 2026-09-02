import { describe, expect, it } from "vitest";
import { incidents, services } from "../src/data.js";
import { IncidentStore } from "../src/incident-store.js";

const fixedNow = "2025-02-12T15:00:00.000Z";

describe("IncidentStore", () => {
  it("summarizes open incidents for every service", () => {
    const store = new IncidentStore(services, incidents);

    expect(store.listServices()).toEqual(expect.arrayContaining([
      expect.objectContaining({ id: "billing-api", openIncidentCount: 1 }),
      expect.objectContaining({ id: "receipt-worker", openIncidentCount: 0 })
    ]));
  });

  it("filters incident summaries without exposing timeline updates", () => {
    const store = new IncidentStore(services, incidents);

    const result = store.listIncidents({ state: "monitoring" });

    expect(result.total).toBe(1);
    expect(result.items[0]).toMatchObject({
      id: "inc-2025-041",
      updateCount: 3,
      latestMessage: "Pool capacity increased; latency is recovering."
    });
    expect(result.items[0]).not.toHaveProperty("updates");
  });

  it("adds a state update using the injected clock", () => {
    const store = new IncidentStore(services, incidents, () => fixedNow);

    const updated = store.addUpdate("inc-2025-037", "monitoring", "Rules restored; watching validation rates.");

    expect(updated).toMatchObject({ state: "monitoring" });
    expect(updated?.updates.at(-1)).toEqual({
      state: "monitoring",
      message: "Rules restored; watching validation rates.",
      createdAt: fixedNow
    });
  });

  it("does not mutate imported fixtures", () => {
    const store = new IncidentStore(services, incidents, () => fixedNow);
    store.acknowledge("inc-2025-041");

    expect(incidents.find(({ id }) => id === "inc-2025-041")?.acknowledgedAt)
      .toBe("2025-02-12T14:11:00.000Z");
  });
});
