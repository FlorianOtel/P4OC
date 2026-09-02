---
id: oa-xyj0
status: closed
deps: []
links: []
created: 2026-07-09T15:15:06Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Redesign Server screen hierarchy

## Problem

`ServerScreen` currently reads like a debug/admin control panel instead of a safe mobile server connection flow. It renders every server concept at once in one vertical scroll: Discovered Servers, Saved servers, Recent Servers, Remote Server form, errors, and Server Setup. There is no progressive disclosure, no clear primary path, and duplicate server rows appear in multiple sections.

## Evidence / Repro

- `app/src/main/java/dev/blazelight/p4oc/ui/screens/server/ServerScreen.kt` renders the main panel order around lines 119-190 as: Discovered -> Saved -> Recent -> Remote form -> error -> Help.
- `local-adb-screenshots/oa-5sro-visual-qa/17_server_removal_open_tabs_warning.png` shows Discovered Servers, Saved servers, Recent Servers, Remote Server, and Server Setup all competing above/below the fold.
- The same endpoint can appear as discovered (`opencode-4096`), saved (`Remote Server`), and recent (`Remote Server`), making the screen look duplicated rather than structured.
- Visual review called this a "wall of monospace" because all panels are same-weight TUI boxes with dense endpoint text and inline actions.

## UX Constraints

- Follow the AGENTS.md Agent-Space UI Rule: prefer contextual, transient, collapsible, or overflow UI over persistent chrome; row-specific actions belong in long-press or overflow menus.
- Server management owns add/edit/remove/auth/reconnect/discovery/certificate policy. Home should only summarize server status where it affects work.
- First-run users need a clear primary action; returning users need fast saved-server selection and safe management.

## Expected Behavior

Split the surface into understandable tasks:

1. **Connect to a server**: primary first-run/disconnected path. Prioritize discovered/saved target if available, otherwise manual URL.
2. **Saved servers / Manage servers**: saved connection targets with friendly names, endpoint, auth/TLS state, connection state, and open-tab count.
3. **Nearby / Discovered**: nearby servers as suggestions, not equal-weight admin panels.
4. **Manual URL**: available but not dominating when saved/discovered targets exist.
5. **Help / Setup**: collapsed by default.

Row tap should connect/select. Secondary actions should move to overflow or a dedicated management/detail sheet.

## Acceptance Criteria

- The top of the screen has one obvious primary connection path.
- Saved, recent, and discovered servers are not shown as confusing duplicate same-weight panels.
- Manual URL remains available but does not dominate when saved/discovered targets exist.
- Server Setup / help content is collapsed or visually secondary.
- Server rows show stable fields: display name, endpoint, connection/auth/TLS status, and open-tab count when relevant.
- Row-specific actions are not always-visible inline text; use overflow/long-press/detail.
- Empty, first-run, saved-server, discovered-server, auth-failure, and open-tabs states are visually distinct.
- Strings are localized and functional controls have content descriptions/test tags.
- Compile and detekt pass.

## Failure States To Avoid

- Do not hide manual URL entirely; users still need explicit server entry.
- Do not make server switching a global mode that loses tab identity; tabs can span servers.
- Do not put destructive actions in the primary tap target.
- Do not add persistent server chrome to Home unless it directly helps work selection.

## Verification

- Capture screenshots for first-run, saved server, discovered server, auth failure, and open-tabs removal-warning states.
- Verify a reviewer can identify the primary action without reading implementation notes.
- Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:detekt`.
