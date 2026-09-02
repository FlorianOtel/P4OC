import { describe, expect, it } from "vitest";
import { fixtureSnapshot } from "../src/fixture";
import { renderDashboard } from "../src/render";

const now = new Date("2025-02-12T15:00:00.000Z");

describe("dashboard rendering", () => {
  it("renders the summary, filtered services, and active incidents", () => {
    document.body.innerHTML = renderDashboard(fixtureSnapshot, {
      region: "North America",
      query: "",
      showResolved: false
    }, now);

    expect(document.querySelector("h1")?.textContent).toBe("degraded");
    expect([...document.querySelectorAll(".service-card h3")].map(({ textContent }) => textContent))
      .toEqual(["Billing API"]);
    expect(document.querySelector(".incident h3")?.textContent)
      .toBe("Elevated payment authorization latency");
    expect(document.body.textContent).not.toContain("Stale inventory facets");
  });

  it("renders resolved incidents when requested", () => {
    document.body.innerHTML = renderDashboard(fixtureSnapshot, {
      region: "all",
      query: "",
      showResolved: true
    }, now);

    expect(document.querySelectorAll(".incident")).toHaveLength(2);
    expect(document.querySelector<HTMLInputElement>("#show-resolved")?.checked).toBe(true);
  });

  it("escapes untrusted service content", () => {
    const unsafe = structuredClone(fixtureSnapshot);
    unsafe.services[0]!.name = `<img src=x onerror="alert(1)">`;
    document.body.innerHTML = renderDashboard(unsafe, {
      region: "all", query: "", showResolved: false
    }, now);

    expect(document.querySelector("img")).toBeNull();
    expect(document.body.textContent).toContain(`<img src=x onerror="alert(1)">`);
  });
});
