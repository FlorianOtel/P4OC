import { describe, expect, it } from "vitest";
import { fixtureSnapshot } from "../src/fixture";
import { availableRegions, filterServices, overallStatus, relativeTime, visibleIncidents } from "../src/status-model";

describe("status model", () => {
  it("derives the most serious current service status", () => {
    expect(overallStatus(fixtureSnapshot.services)).toBe("degraded");
  });

  it("lists unique regions in display order", () => {
    expect(availableRegions(fixtureSnapshot.services)).toEqual([
      "Asia Pacific", "Europe", "Global", "North America"
    ]);
  });

  it("filters services by region and query", () => {
    const results = filterServices(fixtureSnapshot.services, {
      region: "Global",
      query: "inventory",
      showResolved: false
    });

    expect(results.map(({ id }) => id)).toEqual(["catalog-search"]);
  });

  it("hides resolved incidents by default", () => {
    expect(visibleIncidents(fixtureSnapshot.incidents, false).map(({ id }) => id))
      .toEqual(["inc-2025-041"]);
    expect(visibleIncidents(fixtureSnapshot.incidents, true)).toHaveLength(2);
  });

  it("formats recent timestamps for scanning", () => {
    const now = new Date("2025-02-12T15:00:00.000Z");
    expect(relativeTime("2025-02-12T14:41:00.000Z", now)).toBe("19 minutes ago");
    expect(relativeTime("2025-02-12T13:00:00.000Z", now)).toBe("2 hours ago");
  });
});
