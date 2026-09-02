---
id: oa-acdb
status: closed
deps: [oa-s5jj]
links: []
created: 2026-05-05T17:57:03Z
type: feature
priority: 2
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, upload, saf, phase-7]
---
# Tier C: SAF upload via OFISH chunked write

Add an 'Upload here' action in FileExplorerScreen top bar (next to Refresh at FileExplorerScreen.kt:136-156). Uses Android SAF ActivityResultContracts.OpenMultipleDocuments to pick files. Default destination = current explorer path (uiState.currentPath). For v1, no destination picker — user navigates first.

For each picked Uri:
- Read content via ContentResolver.
- Determine MIME via ContentResolver.getType then extension fallback.
- Pipe through FileRepository.upload (which uses OFISH chunked write at 256 KiB raw per chunk).
- Show progress sheet ('Uploading 2 of 5: logo.png — 47 KB / 120 KB').
- On success, refresh the file list.

Salvage from the deleted FileAttachment.kt (~80 LOC of pure helpers) into a NEW package ui/screens/files/upload/UploadVisuals.kt: getFileSymbol, getMimeTypeLabel, formatFileSize, the chip/preview visuals. Rename data model to PendingUpload(uri, name, mimeType, size, targetPath, state).

DO NOT ship: 'Share to opencode' Android intent target, multi-file destination picker, drag/drop. Defer to v1.5.

This is strictly additive to chat attachments — chat keeps using path-based SelectedFile workspace files; SAF brings DEVICE files into the workspace, after which they can be attached via the existing chat picker normally.

## Acceptance Criteria

Upload button visible in FileExplorerScreen. Multi-pick works. Progress UI shows per-file. Failed chunk retries up to 3 times then surfaces error with 'partial preserved at <path>.ofish.upload-<opid>.partial' affordance to discard. UploadVisuals.kt < 100 LOC. Existing chat attachment flow unchanged.


## Notes

**2026-05-05T18:19:53Z**

Per council directive 4: each upload uses an ephemeral session (create → probe → stream → verify → delete). DELETE /session/{id} confirmed at OpenCodeApi.kt:46-50. Concurrent uploads use different sessions. Crash/reconnect = restart from byte 0. Sweep orphan __ofish_* sessions on workspace connect.
