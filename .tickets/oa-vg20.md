---
id: oa-vg20
status: closed
deps: [oa-7wc7]
links: []
created: 2026-05-05T17:48:39Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [files, architecture, phase-2]
---
# FileRepository interface + move FilesViewModel + strict path validation

Introduce data/files/FileRepository.kt (interface) with read/list/write/delete/upload/capabilities. Move FilesViewModel off direct WorkspaceClient calls (FilesViewModel.kt:57-58, 114) onto the new repository. Implement strict path validation in the repository (NOT in viewmodels): reject empty, absolute paths, '..' segments, and post-normalization escapes. The current canonicalFilePath (FilesViewModel.kt:144-146) is too weak for mutation. The repository must be workspace-scoped via WorkspaceClient (no global variants — AGENTS.md forbidden patterns 1, 4, 6, 9). Returns rich result types (sealed: Ok / Conflict / Failed) to support the upcoming hash-guarded write.

## Acceptance Criteria

FileRepository + ShellFileRepository skeleton (write/delete/upload throw NotImplementedError for now — Phase 4 fills them). FilesViewModel reads/lists through repository. Path validation rejects malicious paths in unit tests. App compiles, file viewer/explorer behave identically.


## Notes

**2026-05-05T19:01:31Z**

Implemented and committed in e7c59ba. Added FileRepository/WorkspaceFileRepository + FilePathValidator, moved FilesViewModel entirely off WorkspaceClient including symbol search, preserved root/list/status behavior, added validator/repository tests, updated stale inline-permission tests after cleanup. Verified: ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests DialogQueueManagerTest --tests ChatViewModelTest --tests FilePathValidatorTest --tests WorkspaceFileRepositoryTest (green).
