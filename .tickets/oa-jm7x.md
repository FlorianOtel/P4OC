---
id: oa-jm7x
status: closed
deps: []
links: []
created: 2026-05-09T15:43:01Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Interrupting a run shows raw JSON aborted toast

When the user interrupts/stops an in-progress run via Stop/abort, the resulting toast/snackbar can surface the raw aborted payload as JSON instead of a friendly message. This leaks internal protocol details into the UI and makes an expected user-initiated stop look like an error.\n\nSteps to reproduce:\n1. Start a chat run that produces streaming output.\n2. While the run is in progress, tap Stop / Interrupt.\n3. Observe the toast/snackbar shown after the abort response.\n\nExpected: show a concise human-readable confirmation such as 'Run stopped' / 'Run interrupted', or no toast if the stopped state is already clear.\n\nActual: toast/snackbar contains raw JSON mentioning aborted.\n\nAcceptance criteria:\n- Interrupt/Stop/abort responses are mapped before display.\n- Raw JSON is never shown for expected abort/stop outcomes.\n- User-initiated aborted events are handled distinctly from genuine failures.\n- Unexpected abort failures still show understandable errors without leaking raw response JSON.


## Notes

**2026-05-09T15:52:56Z**

Standardization note: this is a user-facing error hygiene bug. Treat user-initiated abort as an expected state transition, not an error. Do not show raw JSON/protocol payloads in toast/snackbar. Either show no toast when the stopped state is already visible, or show a concise human-readable message such as 'Run stopped'. Preserve meaningful human-readable errors for genuine failures.
