# Incident API agent guide

## Purpose
This repository is the internal source of truth for Northstar service health and incident updates. Keep handlers thin: HTTP parsing belongs in `router.ts`, business rules in `incident-store.ts`, and wire shapes in `types.ts`.

## Working agreements
- Run `npm test` and `npm run build` before considering a change complete.
- Preserve the JSON error shape `{ "error": { "code", "message" } }`.
- Incident timestamps are ISO-8601 UTC strings. IDs are stable lowercase slugs.
- Add store tests for business rules and HTTP tests for status codes or serialization.
- Do not add a database for demo tasks; the in-memory store is intentional.
- Avoid changing seed records unless a task explicitly asks for fixture changes.

## Useful commands
- `npm run dev` — start the API on `PORT` (default 4310)
- `npm test` — run the Vitest suite once
- `npm run build && npm start` — compile and serve production JavaScript
- `curl http://localhost:4310/v1/services` — inspect service summaries
