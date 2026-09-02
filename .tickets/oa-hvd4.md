---
id: oa-hvd4
status: closed
deps: []
links: []
created: 2026-05-07T10:03:00Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, editor, ofish, conflict]
---
# Expose file content hash for editor conflict detection

Sora editor save flow is wired for baselineHash but readFile/FileContentDto currently do not expose a hash, so OFISH stale-write conflicts are not triggerable from normal editor saves. Add an optional hash to file read DTO/domain state and populate it from the same OFISH/server hash source used by writes, then pass it as FileWriteRequest.expectedHash. Acceptance: editing a file, externally modifying it, then saving shows the conflict dialog instead of overwriting.

