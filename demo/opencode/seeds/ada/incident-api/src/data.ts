import type { Incident, Service } from "./types.js";

export const services: Service[] = [
  { id: "checkout-web", name: "Checkout Web", tier: 1, owner: "Commerce Experience", region: "global" },
  { id: "billing-api", name: "Billing API", tier: 1, owner: "Revenue Platform", region: "us-east-1" },
  { id: "receipt-worker", name: "Receipt Worker", tier: 2, owner: "Revenue Platform", region: "eu-west-1" },
  { id: "catalog-search", name: "Catalog Search", tier: 2, owner: "Discovery", region: "global" }
];

export const incidents: Incident[] = [
  {
    id: "inc-2025-041",
    serviceId: "billing-api",
    title: "Elevated payment authorization latency",
    severity: "major",
    state: "monitoring",
    startedAt: "2025-02-12T14:06:00.000Z",
    acknowledgedAt: "2025-02-12T14:11:00.000Z",
    updates: [
      { state: "investigating", message: "P95 authorization latency exceeded 2 seconds.", createdAt: "2025-02-12T14:06:00.000Z" },
      { state: "identified", message: "A saturated connection pool is delaying issuer calls.", createdAt: "2025-02-12T14:23:00.000Z" },
      { state: "monitoring", message: "Pool capacity increased; latency is recovering.", createdAt: "2025-02-12T14:41:00.000Z" }
    ]
  },
  {
    id: "inc-2025-039",
    serviceId: "catalog-search",
    title: "Stale inventory facets",
    severity: "minor",
    state: "resolved",
    startedAt: "2025-02-10T09:20:00.000Z",
    acknowledgedAt: "2025-02-10T09:32:00.000Z",
    resolvedAt: "2025-02-10T11:04:00.000Z",
    updates: [
      { state: "investigating", message: "Inventory facet counts lag product updates.", createdAt: "2025-02-10T09:20:00.000Z" },
      { state: "resolved", message: "Rebuilt the affected search partitions.", createdAt: "2025-02-10T11:04:00.000Z" }
    ]
  },
  {
    id: "inc-2025-037",
    serviceId: "checkout-web",
    title: "Intermittent address validation failures",
    severity: "critical",
    state: "identified",
    startedAt: "2025-02-08T18:45:00.000Z",
    acknowledgedAt: "2025-02-08T18:48:00.000Z",
    updates: [
      { state: "investigating", message: "Some EU addresses fail validation during checkout.", createdAt: "2025-02-08T18:45:00.000Z" },
      { state: "identified", message: "A rules rollout omitted two postal formats.", createdAt: "2025-02-08T19:02:00.000Z" }
    ]
  }
];
