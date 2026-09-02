---
id: oa-qr52
status: closed
deps: [oa-7ysx, oa-vvep, oa-0f4m, oa-cemz, oa-blgp, oa-ww0m]
links: []
created: 2026-05-01T17:45:54Z
type: task
priority: 1
assignee: Jasmin Le Roux
parent: oa-gt0g
tags: [workspace, primitives]
---
# Commit 1: ADD workspace/session/path primitives

Add domain/workspace/{Workspace,WorkspacePath,AttachmentRef}.kt and domain/session/{SessionId,WorkspaceSession}.kt. Add data/server/{ActiveServerApiProvider,ServerEventGateway}.kt. Add data/workspace/WorkspaceClient.kt (wraps OpenCodeApi, bakes directory in). Add data/session/{SessionRepository interface, SessionRepositoryImpl skeleton, SessionReducer}.kt. No usages yet — must compile standalone.

## Acceptance Criteria

1) All files exist in target packages per plan. 2) RelativePath constructor rejects blank/absolute/file://. 3) Workspace has NO companion object with default values (no Workspace.global / .DEFAULT / .current). 4) WorkspaceClient has 'val workspace' (immutable), no 'var workspace'. 5) Unit tests on RelativePath, WorkspacePath, AttachmentRef pass. 6) ./gradlew :app:compileDebugKotlin green.

