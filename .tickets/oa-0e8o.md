---
id: oa-0e8o
status: closed
deps: []
links: []
created: 2026-05-07T14:48:05Z
type: bug
priority: 1
assignee: Jasmin Le Roux
tags: [files, editor, license, cleanup]
---
# Corrective batch for oa-hvd4 / oa-lmh0 reward hacks

Council audit (post-oa-3rk7) flagged reward hacks: 1) WorkspaceFileRepository.toDomain drops FileContentDto.hash so baseline conflict detection is dead in default API path; 2) OfishBaselineHasher digests in-memory String not on-disk bytes producing false 409s on CRLF/BOM files; 3) Termux GPL-3.0 libs lack conveying-source notice (relinking notice covers LGPL only); 4) MIT grammar LicenseEntries marked version=null which catalogue doc reserves for 'planned, not shipped'; 5) Stale FilesViewModel comment claims read DTO has no hash; 6) confirmSave Ok branch bumps contentGeneration unnecessarily wiping cursor/undo on save; 7) _themeTypeAnchor dead val.

## Acceptance Criteria

DTO.hash maps through WorkspaceFileRepository, OFISH baseline matches on-disk hash (or caveat is honest about scope), GPL notice present, MIT entries versioned, dead code removed, tests cover real adapter not fake-only.

