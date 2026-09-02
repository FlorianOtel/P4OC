---
id: oa-0mel
status: closed
deps: []
links: [oa-dygk, oa-wmvc, oa-3yk2, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 2
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Resource tab and slash command display metadata

Problem:
Tab titles and slash command descriptions include user-facing hardcoded display metadata outside a dedicated resource/UI boundary. This overlaps but is not identical to command dispatch correctness.

Evidence:
Display-boundary audit identified TabBar.kt route/tab titles and ChatViewModel.kt/SlashCommandsPopup.kt built-in slash command descriptions as user-visible copy. oa-3yk2 owns command dispatch semantics; oa-dygk owns popup placement/metadata UX. This ticket owns resource-backed display text for tab/command metadata.

UX Constraint:
Tab and command metadata must remain compact enough for phone screens and must not take persistent space unnecessarily. Labels/descriptions should be clear, localized where possible, and consistent between typed slash suggestions and command palette.

Expected Behavior:
Tabs and slash commands expose stable ids/semantic types; UI maps them to resource-backed labels/descriptions. Server/custom command descriptions remain upstream-provided content and are not overwritten by Android hardcoded text unless classified as local built-ins.

Acceptance Criteria:
- Move Android local tab titles and local built-in command descriptions to resources or centralized UI formatter.
- Preserve upstream command descriptions for server/custom/MCP/skill commands.
- Coordinate with oa-3yk2 so only commands classified as local built-ins receive Android resource metadata.
- Coordinate with oa-dygk so popup display metadata remains consistent.
- Add tests for local built-in display metadata versus upstream command metadata preservation.

Verification:
Run targeted command metadata/popup/tab title tests and compile after implementation.


## Notes

**2026-07-06T08:57:34Z**

Workspace label source clarification from 2026-07-05 discussion:

TabBar.getTabTitle currently takes workspaceDirectory: String? and builds suffixes from it at TabBar.kt:209-234. After oa-e6g3, TabState should store WorkspaceKey? instead of raw String? workspaceDirectory, so this ticket's tab title/resource work must derive workspace labels from WorkspaceKey rather than nullable directory strings.

Expected label model after oa-e6g3:
- WorkspaceKey.Directory('/path/project-a') -> resource-backed suffix/display label such as 'project-a' and titles like 'Sessions · project-a'.
- WorkspaceKey.Global -> intentional top-level/server-wide tab. Product may choose either no suffix for default top-level views (e.g. 'Sessions') or a resource-backed 'Server-wide' suffix where clarity is needed.
- workspaceKey == null -> legacy/missing workspace recovery label, not the same as Global.
- WorkspaceKey.SessionScoped(sessionId) -> resolve to displayable workspace/session context before title formatting or show a recovery/loading label.

Dependency note:
The tab title source should switch cleanly after oa-e6g3's TabState.workspaceKey change. Until then, avoid further entrenching workspaceDirectory:String? in new title-formatting APIs.

**2026-07-07T20:26:14Z**

Implemented resource-backed tab and slash-command display metadata. Tab title formatting now takes TabTitleLabels populated with stringResource at the MainTabScreen/TabBar UI boundary, with compact tab workspace labels including dedicated tab_workspace_global. Built-in slash commands now carry no ViewModel hardcoded descriptions; builtInCommandDescriptionRes maps local built-ins to R.string.slash_command_* resources, and Compose boundaries resolve those descriptions for ChatInputBar slash filtering/suggestions and CommandPalette while preserving custom/MCP/skill/upstream descriptions. Slash suggestions now display the resolved description line consistently with the palette. Added CommandMetadataTest for built-in resource mapping, unknown fallback, and preservation of custom/MCP/skill/unknown-built-in descriptions; added TabBarTitleTest for route/workspace title cases. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.components.command.CommandMetadataTest --tests dev.blazelight.p4oc.ui.tabs.TabBarTitleTest; ./gradlew :app:compileDebugKotlin.
