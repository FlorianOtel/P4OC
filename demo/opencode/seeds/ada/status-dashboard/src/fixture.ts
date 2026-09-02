import type { StatusSnapshot } from "./types";

export const fixtureSnapshot: StatusSnapshot = {
  generatedAt: "2025-02-12T14:48:00.000Z",
  services: [
    { id: "checkout-web", name: "Checkout Web", description: "Cart, address, and order confirmation", region: "Global", status: "operational", uptime30d: 99.98 },
    { id: "billing-api", name: "Billing API", description: "Payment authorization and capture", region: "North America", status: "degraded", uptime30d: 99.91 },
    { id: "receipt-worker", name: "Receipt Delivery", description: "Email and PDF receipt generation", region: "Europe", status: "operational", uptime30d: 99.99 },
    { id: "catalog-search", name: "Catalog Search", description: "Product discovery and inventory facets", region: "Global", status: "operational", uptime30d: 99.95 },
    { id: "merchant-console", name: "Merchant Console", description: "Order administration for merchants", region: "Asia Pacific", status: "maintenance", uptime30d: 99.88 }
  ],
  incidents: [
    {
      id: "inc-2025-041",
      title: "Elevated payment authorization latency",
      severity: "major",
      state: "monitoring",
      affectedServiceIds: ["billing-api"],
      startedAt: "2025-02-12T14:06:00.000Z",
      updates: [
        { message: "We increased connection capacity and authorization latency is recovering.", publishedAt: "2025-02-12T14:41:00.000Z" },
        { message: "A saturated connection pool was delaying issuer calls.", publishedAt: "2025-02-12T14:23:00.000Z" }
      ]
    },
    {
      id: "inc-2025-039",
      title: "Stale inventory facets",
      severity: "minor",
      state: "resolved",
      affectedServiceIds: ["catalog-search"],
      startedAt: "2025-02-10T09:20:00.000Z",
      updates: [
        { message: "Search partitions were rebuilt and inventory counts are current.", publishedAt: "2025-02-10T11:04:00.000Z" }
      ]
    }
  ]
};
