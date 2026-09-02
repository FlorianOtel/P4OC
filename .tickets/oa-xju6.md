---
id: oa-xju6
status: closed
deps: []
links: []
created: 2026-07-08T14:42:17Z
type: epic
priority: 1
assignee: Jasmin Le Roux
---
# Pinned Home and multi-server Start Work architecture

Implement the product/architecture direction for P4OC where a non-closeable pinned Home surface opens/resumes existing work, the + affordance creates/opens new work from current context, tabs can span multiple servers/workspaces, and server configuration/lifecycle is managed separately.

Current app reality to preserve:
- Users connect through ServerScreen and currently navigate into Sessions/Chat-oriented work.
- MainTabScreen restores persisted tabs and creates a Sessions tab when none are restored.
- Current + menu creates New Sessions tab, New Files tab, or New Terminal tab.
- Sessions screen owns search, quick-create, project grouping, expand/collapse, session actions (rename/delete/share/summarize/view changes), and session click into Chat.
- Chat/Files/Terminal tabs are contextual and should keep explicit workspace identity.

Target mental model:
- Home = pinned, non-closeable surface for existing/resumable work: connected servers, recent workspaces, workspace detail, filtered sessions, existing tabs.
- + = context-fast creation/opening of new work: new chat, files tab, terminal, or choose another target.
- Servers = configuration/credential/lifecycle management, not a mode users switch through to work.
- Notifications/attention remain badges/dots on Home/tabs/server/workspace indicators, not a notification feed.

## Acceptance Criteria

- All child tickets are either closed or explicitly superseded.
- Fresh-session implementer can follow ticket dependency order without hidden conversation context.
- Final implementation preserves existing Chat/Files/Terminal/Sessions functionality while replacing the weird New Sessions tab flow with pinned Home + Start Work.
- Mixed server/workspace identity is explicit for every tab/action; no hidden global current-server fallback is introduced.

