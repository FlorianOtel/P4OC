---
id: oa-dygk
status: closed
deps: []
links: [oa-0mel, oa-casy, oa-3yk2, oa-wmvc, oa-qy0f, oa-wxf2]
created: 2026-05-09T15:47:12Z
type: feature
priority: 2
assignee: Jasmin Le Roux
---
# Improve slash command popup placement and metadata

The slash command autocomplete popup can cover the current command while typing and does not clearly distinguish built-in, skill, MCP, or custom command sources. Upstream opencode anchors the popover above the prompt input and shows source badges.\n\nExpected UX:\n- Slash autocomplete never obscures the typed command or cursor.\n- Empty results are shown explicitly instead of making the popup disappear.\n- Commands display useful source metadata such as skill/MCP/custom where available.\n\nAcceptance criteria:\n- Popup is anchored above the chat input or otherwise positioned so typed text remains visible.\n- Popup handles IME/inset changes on common phone sizes.\n- Empty state is visible for unmatched input such as '/term'.\n- Skill/MCP/custom/built-in source metadata is shown when available.\n- Keyboard/dpad navigation and active-item visibility are preserved or improved.


## Notes

**2026-05-09T15:52:34Z**

Standardization note: deliver complete slash popup UX, not phased partial polish. Include placement, metadata, and interaction quality together: never cover typed command/cursor; handle IME/insets on phone sizes; show empty state for unmatched commands; show skill/MCP/custom/built-in source metadata when available; keep active item visible during keyboard/dpad navigation; preserve workspace scoping. UI chrome rule: popup is justified only as contextual autocomplete while typing '/', should disappear outside that context, and must be anchored to avoid consuming agent transcript space unnecessarily.

**2026-05-10T15:51:10Z**

Design constraint: slash autocomplete rows should be extremely compact and one-line. Prefer showing /name plus a short source badge, with description/agent/model omitted or heavily truncated when space is tight. The popup should prioritize keeping the typed command visible and preserving transcript space over showing full metadata.

**2026-07-07T20:46:47Z**

Verified and completed slash command popup placement and metadata behavior. Popup is anchored above the chat input via AboveAnchorPopupPositionProvider, keeps active item visible, shows explicit loading/error/empty states with resource-backed copy, displays compact source badges for built-in/skill/MCP/custom/subtask commands, and now consumes resolved descriptions from oa-0mel for search/display. Exposed pure helpers for filtering/source labels and added SlashCommandsPopupTest coverage for empty/name/description filtering, all source labels, and above-anchor/clamped popup geometry. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.components.chat.SlashCommandsPopupTest; ./gradlew :app:compileDebugKotlin; ./gradlew :app:detekt (fails only on pre-existing SessionListViewModel LongMethod, ConnectionManager ReturnCount, and SessionRepositoryImplTest line-length findings; no SlashCommandsPopup findings remain).
