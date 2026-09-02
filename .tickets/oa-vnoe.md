---
id: oa-vnoe
status: closed
deps: []
links: []
created: 2026-05-10T11:41:15Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Unify session status indicator semantics

Problem:
Status indicators are overloaded across the app. Backend SessionStatus, tab SessionConnectionState, and per-screen rendering each define similar-but-different meanings. The most confusing case is AWAITING_INPUT: ChatViewModel currently derives it from unread response state, so the warning/attention dot means "there is an unread response" rather than "the agent is blocked waiting for user input." This makes warning indicators noisy and inconsistent with the Settings status legend.

Evidence:
- domain/model/Event.kt defines backend SessionStatus as Idle, Busy, Retry.
- domain/model/SessionConnectionState.kt defines ACTIVE, BUSY, AWAITING_INPUT, IDLE, BACKGROUND, ERROR and maps colors in SessionStateColors. ACTIVE and BUSY both map to primary; AWAITING_INPUT maps to warning; IDLE maps to muted; BACKGROUND maps to subtle; ERROR maps to error.
- ui/screens/chat/ChatViewModel.kt derives sessionConnectionState as BUSY from isBusy/running tools/streaming text, AWAITING_INPUT from _hasUnreadResponse, else IDLE. This means AWAITING_INPUT is actually unread notification state, not a pending permission/question/tool approval state.
- ui/tabs/TabBar.kt renders tab dots directly from SessionConnectionState and applies pulse/attention badge behavior for AWAITING_INPUT.
- ui/screens/sessions/SessionListScreen.kt has a separate SessionStatusIndicator that renders Busy as spinner + text, Retry as red refresh + text, and Idle as raw Text("●") with success color.
- ui/screens/settings/SettingsScreen.kt includes a status legend describing a unified language, but the implementation is split across multiple mappings and raw glyphs.

UX Constraint:
The app core value is agent/chat/code workspace space, especially on phones. Do not add persistent chrome. Use dot-only indicators in cramped surfaces such as the tab bar and chat header; use dot+label only in roomier surfaces such as session list rows and Settings help. Reserve motion for states that genuinely need attention. Functional indicators need content descriptions and should use shared components rather than raw glyphs.

Expected Behavior:
Use one canonical UI-facing status model for session presence/attention, derived from backend SessionStatus plus local UI signals. Backend SessionStatus remains a wire/runtime concept and should not be rendered directly by screens. Unread response state must be distinct from true awaiting-user-input state.

Suggested model:
- SessionPresence.Error: connection/transport/session error requiring attention.
- SessionPresence.Retrying: transient retry/reconnect/backend retry state.
- SessionPresence.AwaitingInput: agent is blocked on an explicit user decision such as permission, question, or tool approval. This must not be driven by unread response alone.
- SessionPresence.Busy: agent is actively producing output, streaming text, running tools, or backend status is Busy.
- SessionPresence.Unread: agent response completed and the user has not viewed it.
- SessionPresence.Idle: connected/read/no active work.
- SessionPresence.Background: cold/background session or inactive tab with no recent activity.

Precedence:
Resolve visual status top-down so the highest priority active condition wins:
Error > Retrying > AwaitingInput > Busy > Unread > Idle > Background

Implementation Notes:
- Add a central resolver, for example resolveSessionPresence(backendStatus, signals), where signals include pendingPrompt/pendingPermission/pendingQuestion, isStreaming, runningTools, hasUnread, isFocused/isActiveTab, hasError, and isBackground/cold.
- Replace SessionStateColors with or wrap it in a visual mapping that owns color, glyph/icon, motion, label, and contentDescription for each SessionPresence. Suggested name: SessionStatusVisuals.
- Add shared status components such as StatusDot and StatusRow. StatusRow should make the text label optional so cramped surfaces can use dot-only.
- Migrate TabBar to render the shared status component instead of directly mapping SessionConnectionState colors/pulse/attention badge.
- Replace SessionListScreen.SessionStatusIndicator with the shared component and remove raw Text("●") status rendering.
- Update Settings status legend so it exactly matches the canonical model and visual mapping.
- Audit sub-agent rows, chat header, file dirty markers, and any other status-like UI for scattered raw dots, ad-hoc status colors, or duplicated mappings.
- Keep file dirty/unsaved state visually compatible with the shared language, but do not conflate it with session presence; use warning/accent marker near the edited file title.

Acceptance Criteria:
- A single canonical UI-facing session status/presence model exists and is used by tab bar and session list indicators.
- Unread response state no longer uses the same semantic state as awaiting explicit user input.
- AwaitingInput is only shown when there is an actual pending question, permission, approval, or equivalent user-blocking prompt.
- Busy, retrying, idle, unread, background, and error states have documented visual mappings in one shared location.
- Raw status glyph rendering such as Text("●") is removed from session/tab status UI in favor of shared components.
- Settings -> Help status legend matches the implementation exactly.
- Functional status indicators expose meaningful contentDescription values.
- Key interactive/status surfaces keep or add appropriate testTag coverage where practical.
- Motion is limited to Busy, AwaitingInput, and Retrying; Idle, Unread, Background, and Error are static.
- No additional persistent bars, chips, or large chrome are added to chat or tab surfaces.

Verification:
- Run grep/search for raw status dots and duplicated status color mappings; confirm remaining usages are decorative or justified.
- Run the project build verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin
- Manually verify or add tests for resolver precedence: Error beats Retrying, Retrying beats AwaitingInput, AwaitingInput beats Busy, Busy beats Unread, Unread beats Idle, Idle beats Background.
- Manually verify tab bar, session list, chat header/sub-agent indicators, and Settings legend on narrow/mobile-width layouts.
- Verify unread completed responses produce an unread/accent indication, not a warning awaiting-input indication.
- Verify actual pending question/permission/approval produces warning awaiting-input indication with attention behavior.


## Notes

**2026-05-10T11:47:30Z**

Implemented canonical SessionPresence resolver and shared status components. Migrated chat tab presence derivation so pending question/permission drives AwaitingInput and unread responses use a separate Unread state. Migrated TabBar, SessionListScreen, ChatScreen connection dot, and Settings status legend to shared status visuals. Verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin passes.
