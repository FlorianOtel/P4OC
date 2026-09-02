---
id: oa-ssm2
status: closed
deps: [oa-n86n]
links: []
created: 2026-05-05T17:47:36Z
type: epic
priority: 1
assignee: Jasmin Le Roux
tags: [files, architecture, ofish, fish-inspired]
---
# File ops on Android via OFISH (shell-based) protocol

Deliver file create / write / delete / upload + select-and-copy on Android using the existing POST /session/{id}/shell endpoint behind a clean FileRepository abstraction. Inspired by the FISH protocol (Files transferred over SHell). Two-week client-only plan; zero server changes blocking. Migration to native server endpoints (when they land) is a drop-in DI swap. Sign-off doc: /tmp/opencode-signoff/file-ops-signoff.html

## Acceptance Criteria

All sub-tickets closed. App compiles green. User can: select/copy file body text, create new files, edit and save with hash-guarded conflict detection, delete files, upload device files via SAF. App-driven shell calls live in a dedicated workspace-scoped session, never in user chat. Layer 3 integration tests pass against real opencode serve in CI.


## Notes

**2026-05-07T17:37:11Z**

Sweep 2026-05-07T17:37Z: all child tickets except oa-n86n are closed. Functional acceptance items satisfied: select/copy text, create+edit+save with hash-guarded conflict, delete, SAF upload, dedicated workspace-scoped OFISH sessions (never chat). Outstanding acceptance: 'Layer 3 integration tests pass against real opencode serve in CI' = oa-n86n. Epic stays open until oa-n86n lands.
