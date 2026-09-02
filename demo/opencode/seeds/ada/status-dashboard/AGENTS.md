# Status dashboard agent guide

## Product intent
This is Northstar's public, keyboard-friendly status page. It consumes a small status snapshot and renders entirely in the browser. Keep data normalization in `status-model.ts`, HTML generation in `render.ts`, and browser event wiring in `main.ts`.

## Expectations
- Run `npm test` and `npm run build` before finishing a change.
- Preserve semantic headings, native controls, visible focus, and live-region announcements.
- Treat all snapshot strings as untrusted and pass them through `escapeHtml` before interpolation.
- Do not add a UI framework for tasks in this repository.
- Keep fixture mode working; it makes the demo deterministic when no API is available.
- Tests should assert user-visible output or model behavior, not private implementation details.

## Commands
- `npm run dev` — open the dashboard through Vite
- `VITE_STATUS_API_URL=http://localhost:4310 npm run dev` — use a live API origin
- `npm test` — run unit and DOM rendering tests
- `npm run build` — type-check and create the static production bundle
