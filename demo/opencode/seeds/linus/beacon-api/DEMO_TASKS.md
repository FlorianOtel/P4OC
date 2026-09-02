# Demo tasks

1. **Case-insensitive filters**: reproduce the bug where `?status=OPEN` returns no incidents, normalize accepted filter values, and add HTTP and store tests.
2. **Incident detail endpoint**: add `GET /api/v1/incidents/{id}` with a structured 404 response and tests for found, missing, and URL-encoded IDs.
3. **Service summary**: extend `/api/v1/summary` with counts grouped by service without changing existing totals; update tests across the store and HTTP layers.
4. **Safer writes**: reject unknown fields in `POST /api/v1/incidents`, report all invalid fields in a useful response, and preserve the current success contract.
5. **Fixture search**: find every consumer of incident fixture fields, add an optional `owner` property, and ensure old records remain valid.
