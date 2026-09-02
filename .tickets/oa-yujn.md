---
id: oa-yujn
status: closed
deps: []
links: []
created: 2026-07-09T15:15:06Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Centralize Server screen status glyphs

## Problem

`ServerScreen` scatters raw text glyphs and status/action symbols throughout the UI. This makes statuses ambiguous, inaccessible, hard to localize, and inconsistent with the project status-dot semantics.

## Evidence / Repro

Known raw glyph/action usages in `app/src/main/java/dev/blazelight/p4oc/ui/screens/server/ServerScreen.kt` include:

- `⚙` settings affordance around line 97.
- `✗` clear/remove affordance around line 177.
- `◉` / `○` server type selection around lines 295-301.
- `[ ]` / checked-style text for TLS toggle around lines 314-318.
- `●` saved/discovered status dots around lines 538 and 692.
- `◇` recent server marker around line 603.
- `×` recent server remove action around line 626.
- `→` connect/open affordances around lines 715 and elsewhere.
- `● scanning` status around line 740.
- Raw `remove` / `warn remove` overlaps with oa-zemb.

These appear in screenshots such as `local-adb-screenshots/oa-5sro-visual-qa/17_server_removal_open_tabs_warning.png`, where users see multiple symbols without clear semantic distinction.

## UX Constraints

- Follow AGENTS.md Status Dot Semantics: use one consistent status language across tabs, sessions, sub-agents, chat, files, and settings; prefer centralized mappings over scattered raw `Text("●")` glyphs.
- Functional indicators need meaningful content descriptions and should not rely on shape/text glyph alone.
- Use project theme tokens, `LocalOpenCodeTheme`, `Spacing`, `Sizing`, TUI components, and resource-backed strings.

## Expected Behavior

Server screen status and action indicators come from centralized components/mappings rather than ad hoc `Text` glyphs. Examples:

- `ServerStatusIndicator` or equivalent for connected/disconnected/discovered/scanning/error/open-tabs states.
- `TuiIconButton`/`IconButton` with localized content descriptions for settings, clear, remove, overflow, connect.
- A row overflow/action component for secondary row actions.
- Explicit text labels where symbols are ambiguous.

## Acceptance Criteria

- No functional ServerScreen control/status is represented only by raw `Text("●")`, `Text("◇")`, `Text("×")`, `Text("→")`, `Text("⚙")`, `Text("✗")`, or bracket checkbox text.
- Status meanings are centralized in one mapping/component with named severities/states.
- Functional controls have localized content descriptions and test tags where they are key interactions.
- The visual status language matches AGENTS.md semantics: stable/connected, idle, running, awaiting input/warning, retrying, error, background/coldmuted, dirty.
- Decorative glyphs, if any remain, are not the only semantic carrier.
- Strings are resource-backed.
- Compile and detekt pass.

## Failure States To Avoid

- Do not merely wrap raw glyphs in helper functions without assigning semantics.
- Do not replace every glyph with heavy Material icons if that breaks the TUI density; use compact themed indicators.
- Do not introduce multiple competing status languages for Home, tabs, and server rows.

## Verification

- Grep `ServerScreen.kt` for raw glyph text and confirm functional usages are gone or decorative-only.
- Inspect screenshots for saved/recent/discovered/scan/auth-failure states and confirm status meanings are understandable.
- Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin && JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:detekt`.
