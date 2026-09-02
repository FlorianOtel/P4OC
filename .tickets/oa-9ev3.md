---
id: oa-9ev3
status: closed
deps: []
links: [oa-4olr, oa-x9pe, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Require explicit terminal PTY shell cwd and title defaults

Problem:
Terminal PTY creation uses Android-guessed defaults such as /bin/bash, cwd '.', and title Terminal. These defaults can launch the wrong shell or directory and hide missing workspace context.

Evidence:
Hardcoded-default audit identified PtyDtos.kt CreatePtyRequest defaults: shell/command defaults to /bin/bash, cwd defaults to '.', and title defaults to Terminal.

UX Constraint:
Terminal workflows are core workspace operations. Starting a terminal in the wrong directory or shell can cause destructive wrong-project commands. Defaults must be explicit, workspace-scoped, and human-readable when unavailable.

Expected Behavior:
PTY requests use the selected workspace directory and server/upstream/user-configured shell/title policy. Missing cwd/shell context is surfaced or intentionally server-delegated; Android should not silently invent a global cwd or shell.

Acceptance Criteria:
- Remove or replace DTO-level /bin/bash, '.', and Terminal defaults with explicit request construction policy.
- Determine source of truth for shell, cwd, and title: server default, workspace directory, user setting, or explicit UI selection.
- Ensure terminal creation requires or derives workspace-scoped cwd intentionally.
- Add tests proving missing workspace/cwd does not silently become '.'.
- Add tests proving shell/title defaults come from the chosen source of truth or are omitted for server defaulting.
- Provide human-readable error/setup UI if a terminal cannot be created due to missing context.

Verification:
Run targeted terminal PTY request tests and compile. Smoke test opening a terminal from a workspace tab.


## Notes

**2026-07-06T08:54:42Z**

Clarification from 2026-07-05 workspace/tab UX discussion:

Coordinate this ticket with oa-e6g3. Top-level terminal creation from MainTabScreen.kt:394-396 currently inherits activeWorkspaceDirectory; the agreed flat-tab model says top-level menu/tab creation must not implicitly inherit the active tab's workspace. For this ticket, that means PTY request construction must distinguish:

1. Top-level terminal creation: use an explicit server/global default policy OR ask the user to choose workspace/server context. Do not use the active tab's directory by omission.
2. Contextual terminal creation from a specific tab/project, e.g. MainTabScreen.kt:498-500: preserve that tab's explicit WorkspaceKey.Directory when constructing cwd/title/request data.
3. Legacy/missing workspace: do not silently map missing cwd to '.'. Surface a human-readable recovery/setup state or intentionally omit cwd for server defaulting if the backend contract supports that.

The existing ticket wording about 'selected workspace directory' should be read as 'explicit terminal context', not 'always require Directory'. WorkspaceKey.Global may be valid for top-level/server-default terminal behavior if product chooses that policy.

**2026-07-06T08:55:20Z**

Concrete PTY callsite clarification from 2026-07-05 discussion:

MainTabScreen.kt currently creates PTYs with zero-arg CreatePtyRequest() at both top-level/contextual paths:
- around MainTabScreen.kt:390 before creating the top-level terminal tab
- around MainTabScreen.kt:494 before creating a terminal from a specific tab/context

Because CreatePtyRequest() has DTO defaults, both paths currently use /bin/bash, cwd '.', and title 'Terminal'. Neither path passes the workspace directory as cwd today.

Required policy with oa-e6g3 WorkspaceKey model:
- WorkspaceKey.Directory terminals: construct CreatePtyRequest explicitly with cwd = directory path (or the product-approved project terminal cwd), not DTO default '.'. This applies to contextual terminal creation from a project/workspace tab.
- WorkspaceKey.Global terminals: there is no workspace directory to use as cwd. Use a verified server-delegated cwd policy or ask the user to choose cwd/context. Do not use '.' as an Android-guessed fallback unless the backend contract explicitly defines it and tests assert that behavior.
- Top-level terminal creation from menu must not implicitly inherit active tab directory; it must choose the Global/server-delegated/user-chosen policy explicitly.

Acceptance addendum:
Tests should cover both MainTabScreen PTY construction paths or their extracted request builder: top-level Global does not send '.', and Directory contextual creation sends the directory cwd explicitly.

**2026-07-07T19:57:28Z**

Completed PTY request policy. CreatePtyRequest DTO no longer supplies Android-guessed /bin/bash, '.', or Terminal defaults. MainTabScreen now routes both top-level and contextual terminal creation through createPtyRequestForWorkspace: WorkspaceKey.Global omits command/cwd/title for server-delegated defaults; WorkspaceKey.Directory sends cwd equal to the directory and a basename title; missing tab workspace identity is refused with a human-readable snackbar instead of guessed. Added MainTabScreenPtyRequestTest covering Global null command/cwd/title/empty args and Directory explicit cwd/title with cwd != '.'. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.tabs.MainTabScreenPtyRequestTest; ./gradlew :app:compileDebugKotlin.
