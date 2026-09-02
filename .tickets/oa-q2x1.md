---
id: oa-q2x1
status: closed
deps: []
links: []
created: 2026-05-09T15:44:01Z
type: feature
priority: 2
assignee: Jasmin Le Roux
---
# Add create-new-session button to project session list

Inside a selected project/workspace, the session list should provide an obvious create-new-session action. Users should not have to back out, use slash commands, or infer another control to start a fresh session within the current project.\n\nExpected UX:\n- When viewing the session list for a project/workspace, show a clear New Session / + button.\n- The action creates a session scoped to the current workspace directory.\n- The new session opens immediately in the current tab/project context.\n- The button should be available in normal and empty-list states.\n\nAcceptance criteria:\n- Project-scoped session list has a visible create-new-session button.\n- Empty project/session-list state includes a create-new-session CTA.\n- Created session uses the current tab/workspace directory; no global/default workspace fallback.\n- UI follows existing theme tokens and accessibility conventions, including content description/test tag for the interactive control.


## Notes

**2026-05-09T15:52:46Z**

Standardization note: create-new-session control must be project/workspace scoped and space-efficient. It should be obvious in the project session list and empty state, but should not add persistent chrome inside the chat/agent transcript. Prefer a compact '+'/New Session action in the list header or empty state. Acceptance must verify no global/default workspace fallback and immediate navigation to the new session in the current project context.

**2026-05-10T12:34:02Z**

Implemented a project-scoped New Session action in filtered session lists and kept the empty-state CTA path covered by the same visible action. The action uses the filtered project's worktree and existing workspace-switch/autocreate flow, then opens the created session via onNewSession. Verified with ./gradlew :app:compileDebugKotlin.
