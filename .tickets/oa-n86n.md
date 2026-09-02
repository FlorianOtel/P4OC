---
id: oa-n86n
status: closed
deps: [oa-s5jj]
links: []
created: 2026-05-05T17:57:03Z
type: task
priority: 2
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, tests, ci, phase-6]
---
# Layer 3 integration tests against real opencode serve in CI

Spin up a real opencode-server in CI Docker. Add app/src/androidTest/ directory if missing. Instrumentation tests use ConnectionManager to point at the dockerized server.

Test scenarios:
- OFISH session created on first file op; reused; not visible in user-facing session list (post client-side filter).
- Capability probe completes against busybox base image AND ubuntu:22.04 AND macOS-style shasum env (use a shim if needed).
- Write small file, read back, hash matches.
- Overwrite with correct expectedHash → 200; with stale → 409 conflict dialog.
- Delete existing → 204; delete missing → 404.
- Upload 1 MiB binary in chunks → final hash matches client-computed pre-upload hash.
- Concurrent writes from two pretend clients (or two OFISH sessions) — second one hits 409.
- Capability missing scenario: stub probe response with 501; UI disables Save/Delete/Upload.
- Permission auto-approval: matching callID auto-approves once; unmatched callID surfaces dialog.

Run nightly initially. Promote to required pre-merge once stable.

## Acceptance Criteria

CI Dockerfile committed under app/src/androidTest/docker/. Workflow runs the full scenario list nightly and reports green. At least one alpine/busybox + one debian + one macOS-shim image covered.

