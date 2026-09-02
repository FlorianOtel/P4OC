---
id: oa-wfgc
status: closed
deps: []
links: []
created: 2026-07-05T18:04:23Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Fix built-in slash command semantics beyond undo redo

Problem:
Android hardcodes TUI/client built-in slash commands and mixes them with upstream server commands. This creates incorrect semantics and can shadow custom/server/MCP/skill commands.

Evidence:
Audit found built-in command logic around ChatViewModel.kt, ChatScreen.kt, WorkspaceClient.kt, and command tests. Beyond the existing undo/redo ticket oa-3yk2, likely problematic commands include /compact, missing /summarize alias, /clear, /new, /share, /unshare, /help, /connect, and /bug. Hardcoded commands are prepended before server commands and de-duplicated by name, which means local hardcoded metadata can win over upstream truth.

UX Constraint:
Slash commands must feel consistent with opencode/TUI behavior while preserving Android-specific local actions. The command palette must not mislead users by showing a command that silently executes the wrong endpoint or shadows a project/server command.

Expected Behavior:
Each slash command is classified explicitly as local UI action, session route action, upstream server command, or unsupported/degraded. Typed slash input and command-palette selection must use the same dispatcher. Unsupported commands must show a human-readable message rather than being sent to a wrong endpoint.

Acceptance Criteria:
- Introduce or extend a command dispatcher that classifies built-ins separately from server-provided commands.
- Define correct Android behavior for /compact, /summarize, /clear, /new, /share, /unshare, /help, /connect, and /bug.
- Preserve server/custom/MCP/skill commands without hardcoded Android metadata shadowing upstream definitions.
- Typed slash execution and command palette execution share the same routing path.
- Add tests for typed and palette paths for every classified built-in command.
- Link with oa-3yk2 and oa-dygk so undo/redo and popup metadata work do not diverge.

Verification:
Run targeted ChatViewModel/command-palette unit tests for typed and palette command routes. Manually smoke test command palette search/display where feasible. Compile after implementation with JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin.


## Notes

**2026-07-05T18:05:35Z**

Superseded by oa-3yk2. Broader command findings were folded into oa-3yk2 as an add-note so the explicit dispatcher/classification fix covers all built-ins without a duplicate category ticket.
