import { createServer } from "node:http";
import { incidents, services } from "./data.js";
import { IncidentStore } from "./incident-store.js";
import { createApp } from "./router.js";

const port = Number(process.env.PORT ?? 4310);
if (!Number.isInteger(port) || port < 1 || port > 65_535) {
  throw new Error(`Invalid PORT: ${process.env.PORT}`);
}

const store = new IncidentStore(services, incidents);
const server = createServer(createApp(store));
server.listen(port, "0.0.0.0", () => {
  console.log(`Northstar incident API listening on http://0.0.0.0:${port}`);
});
