---
id: oa-s5jj
status: closed
deps: [oa-p829]
links: []
created: 2026-05-05T17:57:03Z
type: feature
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, ofish, phase-4]
---
# OfishCommandBuilder + OfishFileRepository (heredoc-stdin payloads) + Layer 1+2 tests

Implement OFISH command generation. OfishCommandBuilder is a pure function (op, path, content?, expectedHash?) -> String. Each command starts with '#OFISH_<OP> path=... expected=...' marker (a shell comment that's harmless to POSIX shell but recognizable to a future server-side fast-path matcher), followed by the equivalent POSIX shell body. This is the FISH lesson lifted literally: same wire format covers slow-path (shell) and fast-path (native) eras — there is NO second repository implementation.

Wire format details:
- Single-quote escaping for paths ('\''-style); reject empty/absolute/'..' (also enforced in FileRepository per Phase 2).
- 'set -efu', trap-cleanup for temp files, mkdir -p for parents, atomic mv -f -- in same directory (NOT /tmp — cross-FS rename loses atomicity). printf '%s' not echo.
- Base64 payload fed via HEREDOC ON STDIN ("base64 -d > \"$TMP\" <<'__OFISH_B64__'"), NOT via argv. This sidesteps argv length limits entirely and lets us use larger chunks.
- Heredoc terminator includes a session-random suffix per command (defense-in-depth against payload collision; base64 alphabet excludes '_' anyway).
- Reply parser anchors on '^### \d{3}' (FISH-style FTP-superset trailer): 200 ok, 201 created, 204 deleted, 404 missing, 409 conflict actual=..., 412 precondition, 413 too_large (client-side guard only), 500 error msg=..., 501 caps_missing.
- Conflict and other expected outcomes return shell exit 0 with a non-2xx '### NNN' line. Exit non-zero is reserved for shell internal failures.

Chunked upload: three commands #OFISH_UPLOAD_INIT / #OFISH_UPLOAD_CHUNK n=i / #OFISH_UPLOAD_FINISH. 256 KiB raw per chunk (~342 KiB base64). Hard cap 16 MiB total — refuse with '### 413 too_large' client-side before sending. OFISH session bloats with each command; opportunistically clear or rotate the session above a size threshold.

Crash recovery: orphaned <path>.ofish.upload-<opid>.partial files cleaned during next capability probe.

OfishFileRepository wires the builder through OfishSessionProvider + WorkspaceClient.executeShellCommand. Replaces previously-closed oa-e07q which assumed argv-bounded 64 KiB chunks and a dual-impl architecture.

## Acceptance Criteria

All Layer 1+2 tests pass on Linux + macOS CI. Manual phone test: write a small text file, read back, hash matches; conflict on stale-hash overwrite triggers conflict dialog; delete works; chunked write of a 5 MiB file completes. No regressions in existing FilesViewModel read flow. OFISH marker comments visible in command body (verify by inspection of OFISH session).


## Notes

**2026-05-05T18:19:53Z**

Per council directive 3 + 5: drop the 16 MiB hard cap. Empirical chunk-size benchmark (separate ticket) produces a constant; runtime probe at workspace connect halves only if constant fails — never caps upward. SSE replay rationale dropped (ephemeral sessions are never reconnected to).
