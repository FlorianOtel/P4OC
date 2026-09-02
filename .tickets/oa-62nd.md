---
id: oa-62nd
status: closed
deps: []
links: []
created: 2026-07-05T18:04:23Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Move hardcoded UI display text to resource-backed boundaries

Problem:
Audit found multiple display strings generated outside Android resource/UI formatting boundaries. This repeats the permission-title bug pattern and makes localization, consistency, and testing harder.

Evidence:
Known permission case is tracked in oa-wmvc. Additional audited examples include NotificationHelper.kt notification channel/title/body strings, ChatViewModel.kt built-in command descriptions, SlashCommandsPopup.kt labels/metadata, TodoTracker.kt status/progress labels, SkillsScreen.kt and MCP/skills status text, SoraLanguageRegistry.kt language fallback labels such as plain text, and TabBar.kt route/tab titles.

UX Constraint:
User-facing copy should be localizable and consistent. Functional status text must be meaningful and should not leak protocol/internal names unless intentionally shown as technical metadata.

Expected Behavior:
Domain/protocol/data layers expose raw facts and stable identifiers. UI or notification-specific formatters choose resource-backed strings at the presentation boundary. Tests assert raw domain state separately from UI-rendered localized text.

Acceptance Criteria:
- Inventory audited hardcoded display strings and separate true user-facing strings from protocol/debug/internal identifiers.
- Move user-facing strings into resources or centralized UI formatter functions.
- Keep domain/data models free of localized/display titles except where the type is explicitly presentation-only.
- Update tests that currently assert English text below the UI/resource boundary.
- Ensure notification channel/title/body strings remain human-readable and resource-backed.
- Preserve technical identifiers where they are intentionally shown to users, with labels explaining context.

Verification:
Run targeted unit tests for formatters and affected UI logic. Run resource/build compile after implementation. Detekt should not gain new hardcoded-string or unused-resource findings.


## Notes

**2026-07-05T18:05:35Z**

Superseded by narrower UI-surface display-boundary tickets to be created under oa-nwha. A single broad display-boundary ticket is too vague because notifications, todo labels, MCP/skills status, file language fallback, and tab/slash titles have separate user-facing behavior and verification.
