import type { IncomingMessage, RequestListener, ServerResponse } from "node:http";
import { readJson, sendError, sendJson } from "./http.js";
import { IncidentStore } from "./incident-store.js";
import { incidentStates, type IncidentFilters, type IncidentState } from "./types.js";

function isIncidentState(value: unknown): value is IncidentState {
  return typeof value === "string" && incidentStates.includes(value as IncidentState);
}

async function handleIncidentAction(
  request: IncomingMessage,
  response: ServerResponse,
  store: IncidentStore,
  incidentId: string,
  action: string
): Promise<void> {
  if (action === "acknowledge") {
    const incident = store.acknowledge(incidentId);
    if (!incident) return sendError(response, 404, "incident_not_found", `No incident named ${incidentId}`);
    return sendJson(response, 200, incident);
  }

  if (action === "updates") {
    let body: unknown;
    try {
      body = await readJson(request);
    } catch {
      return sendError(response, 400, "invalid_json", "Expected a small JSON request body");
    }
    if (typeof body !== "object" || body === null) {
      return sendError(response, 422, "invalid_update", "Update must be a JSON object");
    }
    const { state, message } = body as Record<string, unknown>;
    if (!isIncidentState(state) || typeof message !== "string" || message.trim().length < 8) {
      return sendError(response, 422, "invalid_update", "A valid state and descriptive message are required");
    }
    const incident = store.addUpdate(incidentId, state, message.trim());
    if (!incident) return sendError(response, 404, "incident_not_found", `No incident named ${incidentId}`);
    return sendJson(response, 201, incident);
  }

  sendError(response, 404, "route_not_found", "No matching route");
}

export function createApp(store: IncidentStore): RequestListener {
  return (request, response) => {
    void (async () => {
      const url = new URL(request.url ?? "/", "http://incident-api.local");
      const segments = url.pathname.split("/").filter(Boolean);

      if (request.method === "GET" && url.pathname === "/health") {
        return sendJson(response, 200, { status: "ok" });
      }
      if (request.method === "GET" && url.pathname === "/v1/services") {
        return sendJson(response, 200, { items: store.listServices() });
      }
      if (request.method === "GET" && url.pathname === "/v1/incidents") {
        const filters: IncidentFilters = {};
        const serviceId = url.searchParams.get("service");
        const state = url.searchParams.get("state");
        const rawLimit = url.searchParams.get("limit");
        if (serviceId) filters.serviceId = serviceId;
        if (state) {
          if (!isIncidentState(state)) return sendError(response, 400, "invalid_state", `Unknown state: ${state}`);
          filters.state = state;
        }
        if (rawLimit) {
          const limit = Number(rawLimit);
          if (!Number.isInteger(limit) || limit < 1 || limit > 100) {
            return sendError(response, 400, "invalid_limit", "Limit must be an integer from 1 to 100");
          }
          filters.limit = limit;
        }
        return sendJson(response, 200, store.listIncidents(filters));
      }
      if (request.method === "GET" && segments.length === 3 && segments[0] === "v1" && segments[1] === "incidents") {
        const incident = store.getIncident(segments[2] ?? "");
        return incident
          ? sendJson(response, 200, incident)
          : sendError(response, 404, "incident_not_found", `No incident named ${segments[2]}`);
      }
      if (request.method === "POST" && segments.length === 4 && segments[0] === "v1" && segments[1] === "incidents") {
        return handleIncidentAction(request, response, store, segments[2] ?? "", segments[3] ?? "");
      }
      return sendError(response, 404, "route_not_found", "No matching route");
    })().catch((error: unknown) => {
      console.error("request failed", error);
      if (!response.headersSent) sendError(response, 500, "internal_error", "Unexpected server error");
      else response.end();
    });
  };
}
