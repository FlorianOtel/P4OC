---
id: oa-ryve
status: closed
deps: []
links: []
created: 2026-05-05T17:48:39Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, ofish, phase-3]
---
# OFISH capability probe + AppShellSessionProvider + permission broker

Capability probe: on first WorkspaceClient connect, run a probe shell command that emits OFISH/1 caps=base64=...,hash=...,mv,mkdir,rm and exit code. Cache result on WorkspaceClient (per-workspace, per-generation — AGENTS.md compliance). If caps missing, FileRepository.write/delete/upload throw a typed CapabilitiesUnavailable error and UI disables Save/Delete/Upload with a banner explaining why. AppShellSessionProvider: lazily creates and caches a per-workspace session titled '__opencode_app_fileops__' for app-driven shell. NEVER use the user's visible chat session (executeShellCommand returns a MessageWrapperDto that becomes a real chat message — this would pollute history with base64 blobs). Session is filtered out of visible session list (client-side title prefix filter until server adds metadata). Permission broker: when an app shell command emits '### op=<uuid>' marker, correlate incoming OpenCodeEvent.PermissionRequested by op-id; if it matches a user-initiated Save/Delete/Upload, auto-reply 'once'. NEVER auto-reply 'always'. Mismatches surface a confirmation dialog. Falls back to one-prompt-per-session if op-id correlation isn't available.

## Acceptance Criteria

First-connect probe completes, caps cached. Hidden session created on first file op, reused thereafter. Hidden session does not appear in user's session list (or does, with clear marker label, until filter API exists). Op-id correlated permissions auto-approve only matching ops. Layer 2 tests cover: probe parsing for GNU/macOS/BSD, missing tool detection, op-id mismatch flow.


## Notes

**2026-05-05T17:55:45Z**

Closed: superseded by rewritten ticket. Original spec referenced an app-generated op-id and AppShellSessionProvider; revised plan reuses server-issued callID via existing pendingPermissionsByCallId, and renames the session helper to OfishSessionProvider. See /tmp/opencode-signoff/file-ops-signoff.html §5.
