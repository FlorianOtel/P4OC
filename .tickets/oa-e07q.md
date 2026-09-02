---
id: oa-e07q
status: closed
deps: []
links: []
created: 2026-05-05T17:48:39Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, ofish, phase-4]
---
# ShellCommandBuilder + ShellFileRepository (write/delete/chunked-upload) + Layer 1+2 tests

Implement OFISH command generation. ShellCommandBuilder is a pure function (path, op, content?, expectedHash?) -> String. Single-quote escaping for paths ('\''-style); reject empty/absolute/'..' (also enforced in FileRepository per Phase 2). Use 'set -efu', trap-cleanup for temp files, mkdir -p for parents, atomic mv -f -- in same directory (NOT /tmp — cross-FS rename loses atomicity). Use printf '%s' not echo. Hash portability: sha256sum / shasum -a 256 / openssl dgst, detected via probe. Base64 portability: -d / -D / openssl base64 -d -A. Reply parser anchors on '^### \d{3}' (FISH-style trailer): 200 ok, 201 created, 204 deleted, 404 missing, 409 conflict actual=..., 412 precondition, 413 too_large, 500 error msg=..., 501 caps_missing. Chunked write: init (truncate partial) / append / finalize (atomic rename). Start chunk size 64 KiB raw (~85 KiB base64); hard cap 8 MiB total — refuse with 413 above. Crash recovery: orphaned .ofish.upload-<opid>.partial files cleaned during next capability probe. ShellFileRepository wires all of this through AppShellSessionProvider + WorkspaceClient.executeShellCommand. Layer 1 (golden-string tests) covers all path quoting edge cases. Layer 2 (ProcessBuilder against tmpdir) covers actual shell behavior on Linux + macOS.

## Acceptance Criteria

All Layer 1+2 tests pass on Linux + macOS CI. Manual test on phone: write a small text file, read back, hash matches; conflict on stale-hash overwrite triggers the conflict dialog; delete works; chunked write of a 1 MiB file completes. No regressions in existing FilesViewModel read flow.


## Notes

**2026-05-05T17:55:45Z**

Closed: superseded by rewritten ticket. Original spec referenced ShellCommandBuilder/ShellFileRepository with argv-bounded 64 KiB chunks and dual-impl architecture; revised plan uses heredoc-stdin payloads with 256 KiB chunks under a single OfishFileRepository — the server fast-path is invisible to the client (FISH lesson). See /tmp/opencode-signoff/file-ops-signoff.html §3-§5.
