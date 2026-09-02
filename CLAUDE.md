# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

P4OC (Pocket for OpenCode) is an Android client for [OpenCode](https://github.com/sst/opencode), a terminal-based AI coding assistant. The app connects to a running OpenCode server over HTTP/SSE/WebSocket and lets you chat, browse/edit files, view diffs, run a terminal, and manage sessions from a phone. Package: `dev.blazelight.p4oc`.

The UI is a deliberate terminal aesthetic: flat, monospaced where it matters, 0dp corners everywhere, no stock Material3 look. This is a hard styling constraint, not a preference — see "Theme system" below.

## Build & test commands

Always set `JAVA_HOME` first (Java 17 required):

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

```bash
./gradlew :app:compileDebugKotlin        # fast compile check — use this while iterating
./gradlew :app:assembleDebug             # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest         # unit tests (JVM, no device)
./gradlew :app:detekt                    # static analysis
./gradlew installDebug                   # build + install on connected device
```

Run a single unit test class or method:

```bash
./gradlew :app:testDebugUnitTest --tests "dev.blazelight.p4oc.ui.screens.chat.ChatViewModelTest"
./gradlew :app:testDebugUnitTest --tests "*.ChatViewModelTest.someTestMethod"
```

Instrumented tests (requires a connected device/emulator):

```bash
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest --tests "*.ConnectSmokeTest"
```

Theme-convention lint (checks for `MaterialTheme.colorScheme` usage, raw M3 dialogs, hardcoded `RoundedCornerShape` outside `Theme.kt`):

```bash
./scripts/check_theme_violations.sh
```

Detekt uses `detekt.yml` + `app/detekt-baseline.xml`. Fix or ticket new findings rather than deleting framework-, Compose-, serialization-, reflection-, or resource-referenced code just to silence it.

Release builds (`assembleRelease`, `assembleGithubRelease`) need signing config in `local.properties` (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`) — see README for details. Don't attempt these without that config present.

Debug package id is `dev.blazelight.p4oc.debug` (applicationIdSuffix `.debug`) — account for this in `adb`, deep links, and any device inspection.

`Makefile` has local dev shortcuts (`make run`, `make serve`, `make logcat`) — machine-specific, not the canonical build path.

## Architecture

MVVM + clean architecture layers under `app/src/main/java/dev/blazelight/p4oc/`:

```
core/        Network layer (Retrofit/OkHttp), SSE (LaunchDarkly EventSource), DataStore, connection management
data/        DTOs, mappers (DTO -> domain), repository implementations
di/          Koin modules (single KoinModules.kt — no codegen, chosen specifically for AGP 9 + Kotlin 2.3 compat)
domain/      Domain models, repository interfaces, workspace/session/server identity types
terminal/    Termux terminal-emulator/terminal-view integration
ui/
  components/  Shared TUI widgets, markdown renderer, code blocks, tool-call widgets
  navigation/  NavGraph, route definitions/encoding
  screens/     chat, sessions, projects, settings, terminal, files, diff, setup, server
  tabs/        Multi-tab shell (TabManager, TabState, TabNavHost, TabBar)
  theme/       Theme system (SemanticColors, Spacing, Sizing, Typography, Motion)
  workspace/   WorkspaceViewModel / WorkspaceRepositoryOwner — per-tab scoped state
```

### Workspace/tab model — the load-bearing architecture

The app is multi-tab; each tab owns an independently-scoped **workspace** (a server + directory pair). This replaced an earlier "current directory" global-state design, and the old design's bugs are exactly what the current rules exist to prevent. Read `docs/design-locks/README.md` and its linked lock docs before touching session, workspace, routing, or SSE-event code — they contain the worked examples and are the source of truth over any summary here.

Cross-cutting invariants:

- **No ambient/global context.** No global mutable `currentWorkspace`/`currentSession`/`currentDirectory` anywhere, in any layer.
- **Server is the source of truth.** Local state is a cache; on conflict, server state wins.
- **Identity is explicit and typed.** `ServerRef`, `Workspace`, `WorkspaceSession`, `SessionId`, `RelativePath`, `WorkspacePath` (see `domain/workspace/`, `domain/session/`, `domain/server/`) are the only context primitives — don't invent stringly-typed identity.
- **A `WorkspaceViewModel` owns the lifetime of one tab.** Closing the tab disposes everything scoped to it. No leak-by-key-eviction caches.
- **Repositories are constructed with a workspace-scoped client**, never by reaching into active-tab state from the data layer.
- **Deep links from before the cutover are rejected, not best-effort migrated.**

`AGENTS.md` (also loaded automatically) enumerates ten concrete forbidden patterns with bad/good code pairs (no default/global `Workspace`, no nullable `directory` defaults on API methods, no directory fallback chains, no mutable workspace vars, no `CurrentWorkspace` singleton, no parallel chat message buffers outside `SessionRepositoryImpl`, no global API variants, no nullable `withWorkspace` escape hatches, etc.). Treat that list as binding when writing anything that touches workspace/session/directory routing.

Key source locations (from `AGENTS.md`):

| What | Where |
|------|-------|
| Domain models | `domain/model/` |
| API interface | `core/network/OpenCodeApi.kt` |
| SSE events | `core/network/OpenCodeEventSource.kt` |
| DTOs | `data/remote/dto/` |
| Mappers | `data/remote/mapper/Mappers.kt` |
| Chat UI | `ui/screens/chat/` |
| Terminal | `ui/screens/terminal/` + `terminal/` |
| Theme system | `ui/theme/` |

### Networking

Retrofit + OkHttp for REST. SSE via LaunchDarkly's `okhttp-eventsource` for streaming chat events (`OpenCodeEventSource.kt`). A separate WebSocket path drives the embedded PTY/terminal (reconnects with exponential backoff — see `TESTING.md` for the manual verification steps and expected logcat tags `OpenCodeEventSource`, `PtyWebSocket`, `ConnectionManager`).

### Theme system

Not Material3 theming — a custom system loaded from JSON files in OpenCode's own theme format. ~50 semantic color tokens exposed via `LocalOpenCodeTheme.current`, plus `Spacing.*`, `Sizing.*`, `TuiShapes` (all corners 0dp), `Motion.*`. Bundled themes: catppuccin (+frappe/macchiato), dracula, gruvbox, nord, opencode, tokyonight, xterm.

Rules (enforced by `scripts/check_theme_violations.sh`, not just convention):

- Colors: `LocalOpenCodeTheme.current`, never `MaterialTheme.colorScheme`.
- `MaterialTheme.typography` is fine to use — it *is* the custom typography.
- Dimensions: `Spacing.*` / `Sizing.*` tokens, never hardcoded `.dp`.
- Shapes: `TuiShapes` only.
- No stock Material3 widgets, rounded corners, or elevation shadows unless there is genuinely no alternative.

### Interaction/accessibility conventions

- `role = Role.Button` / `Role.Tab` on actionable `.clickable` modifiers.
- Meaningful `contentDescription` on functional (non-decorative) icons.
- `Modifier.testTag(...)` on key interactive elements — `TESTING.md` has the current tag inventory (`tab_bar`, `chat_input`, `send_button`, etc.) for UI-automation coverage.

### Status-dot semantics

One consistent status vocabulary is used across tabs/sessions/sub-agents/chat/files/settings — prefer a centralized mapping over scattered raw `Text("●")` glyphs. States: connected/idle, running/busy, awaiting input, retrying/reconnecting, error, background/cold, dirty/unsaved. No fake progress percentages for agent work — use real run state (spinner/pulse/text). Update the status legend in Settings → Help if you change these semantics.

### Agent-space UI rule

The chat/agent/code workspace is the app's core value; screen space on phones is precious. Justify any persistent UI chrome heavily — prefer contextual/transient/collapsible/overflow UI, put row actions in long-press/overflow menus, put creation actions in headers/overflow/empty-states rather than floating persistent controls, and keep slash/autocomplete popups from covering the typed command or cursor.

### In-app editing

File viewing/editing stays tabbed inside the app (Sora Editor). Don't add intent-out-to-external-editor flows for core editing — it breaks the tabbed workspace model. External editor interop, if ever added, is optional import/export only, not the primary path.

## Tooling in this repo

- **Ticket tracking**: this project uses `tk`. Run `tk ready` to find available work, `tk show <id>` for details, `tk start <id>` / `tk close <id>` to claim/complete. Tickets live under `.tickets/`. When writing/updating tickets, include full acceptance criteria up front (no "phase 1"/partial-delivery language unless explicitly a spike) — see the "Ticket Quality" section of `AGENTS.md` for the expected shape (Problem/Evidence/UX Constraint/Expected Behavior/Acceptance Criteria/Verification).
- **OpenSpec**: for proposals, new capabilities, breaking changes, or architecture shifts, consult `openspec/AGENTS.md` first rather than coding directly — that's the authoritative spec/proposal process for this repo.
- **Design locks** (`docs/design-locks/`): six locked decisions (A–F) covering deep links/prompt modality, SSE hydrate-race semantics, mutation-on-failure contract, server identity, route encoding, and SSE event routing. These are binding on downstream work; revisiting one means reopening its design ticket, not silently diverging in code.
