---
id: oa-runv
status: closed
deps: [oa-cxp9, oa-agea]
links: []
created: 2026-07-09T15:15:06Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Rewrite Home and Start Work user copy

## Problem

The pinned Home and Start Work model is conceptually right, but current copy explains implementation details instead of helping users decide what to tap. This wastes scarce phone space and weakens the intended mental model.

## Evidence / Repro

Current Home screenshots such as `local-adb-screenshots/oa-5sro-visual-qa/07_connected_home.png` and `12_home_server_workspace_open_work.png` show internal wording like:

- `0 bounded open work items; 0 workspace summaries loaded without chat history.`
- `1 bounded open work item; 1 workspace summary loaded without chat history.`
- `Attention appears as compact badges and dots on Home, tabs, servers, and workspaces — not as a feed.`

The Start Work sheet screenshot `local-adb-screenshots/oa-5sro-visual-qa/08_plus_from_home_start_work.png` has the right actions but uses vague/contextual copy such as:

- `Choose an action. Current server/workspace will be explicit before creation.`

Reviewers found this reads like QA/developer explanation, not a user-facing launcher.

## UX Constraints

- Home means open existing work, resume sessions, and browse workspaces.
- `+` means create/start new work in the current or explicitly selected context.
- Notifications/attention should remain compact badges/dots, not Home feed content or explanatory cards.
- Follow AGENTS.md Agent-Space UI Rule: UI chrome must justify itself by helping work; prefer contextual/transient information over persistent explanation.

## Expected Behavior

Home should tell users what they can do next without exposing data-loading internals. Suggested direction:

- Empty open-work copy: `No open work yet` / `Resume a session or start new work with +.`
- Existing work copy: `1 open item` / `Resume files, terminals, or chats from this workspace.`
- Saved server copy: `Remote Server · connected` / `1 open item`.
- Remove the persistent Attention explanation unless there is actionable attention.

Start Work should make target explicit:

- From an active workspace: `Target: Remote Server · p4oc-alpha`.
- From Home/global: `Choose a target before creating work` or `Target: Remote Server · Global` if a default is known.
- Rows should use short action labels and user outcomes, not implementation notes.

## Acceptance Criteria

- Home no longer shows `bounded`, `workspace summaries loaded`, `without chat history`, or other implementation/data-loader phrasing.
- Empty and populated Home states use task-oriented copy.
- The persistent Attention explanatory card is removed, collapsed, or only shown when there is real actionable attention.
- Start Work sheet always names the target or clearly asks the user to choose one before creation.
- Home still emphasizes existing/resume/browse; `+` still emphasizes create/start.
- Copy is localized in `strings.xml` where applicable.
- Screenshots of Home empty, Home with open work, Start Work from Home, and Start Work from active workspace show the intended mental model without commentary.
- Compile and detekt pass.

## Failure States To Avoid

- Do not turn Home into a notification feed.
- Do not duplicate every `+` creation action as a prominent Home action; Home can provide secondary start affordances but must primarily open existing work.
- Do not hide Browse Sessions; Sessions remains the search/history/actions surface.
- Do not use vague target copy that makes users wonder which server/workspace will be affected.

## Verification

- Inspect Home and Start Work screenshots before/after the copy change.
- Grep source for banned internal phrases: `bounded`, `without chat history`, `workspace summaries loaded`, `Current server/workspace will be explicit`.
- Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:detekt`.

## Verification Notes

- 2026-07-09: Replaced Home implementation copy with task-oriented copy. Banned phrases `bounded`, `without chat history`, `workspace summaries loaded`, and `Current server/workspace will be explicit` no longer appear in `HomeScreen.kt` or `MainTabScreen.kt`.
- 2026-07-09: Start Work now shows an explicit target when a server/default workspace exists, or asks the user to choose a target before creating work.
- 2026-07-09: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:detekt` passed.
- 2026-07-09: Home was structurally changed toward the approved mockup: compact header, Servers cards, Resume workspace rows, Browse actions, and no persistent Attention explainer block.
- 2026-07-09: Device screenshot verification is still required before closing because `adb devices` currently has no connected device. Capture Home empty, Home with open work, Start Work from Home, and Start Work from active workspace before closing.

## Notes

**2026-07-10T07:06:56Z**

Device verification completed for the remaining feasible copy states: ux-final-home.png shows task-oriented empty Home without implementation phrases or attention explainer; ux-final-start-work.png clearly requests server/workspace selection; ux-final-start-work-scoped.png names endpoint plus explicit No project context and separates new from existing work. Integrated compile, detekt, and unit suite pass.
