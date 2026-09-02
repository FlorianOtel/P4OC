---
id: oa-e6gu
status: closed
deps: []
links: []
created: 2026-05-09T15:47:05Z
type: bug
priority: 2
assignee: Jasmin Le Roux
---
# Make slash command loading resilient

Slash commands/skills can fail to appear because commands are loaded lazily only when input starts with '/' and the current command list is empty. If the API fails once, built-in commands are cached and the UI may never retry loading workspace commands/skills until restart.\n\nExpected UX:\n- Opening the slash command popup should refresh or retry command loading when needed.\n- A failed load should not permanently trap the UI in built-in-only mode.\n- Users should see loading/error/empty states instead of silent disappearance.\n\nAcceptance criteria:\n- Command loading can retry after API failure.\n- Built-in fallback does not suppress future custom/skill/MCP command refreshes.\n- Slash popup shows clear loading, empty, and recoverable error states.\n- Skills/custom/MCP commands appear when returned by the server for the current workspace.\n- Behavior remains workspace-scoped; no global/default workspace fallback.


## Notes

**2026-05-09T15:52:29Z**

Standardization note: ticket should carry full context. Root cause to verify: slash commands are loaded only when input starts with '/' and command list is empty; one API failure can cache built-ins and suppress future custom/skill/MCP refresh. Expected behavior: workspace-scoped commands refresh/retry reliably; built-in fallback never prevents later server commands; loading/error/empty states are visible; raw failures are human-readable. UI chrome must be minimal and justified: popup exists only while slash input is active and must not steal agent transcript space outside that mode.

**2026-05-10T11:24:28Z**

Made slash command loading retryable and workspace-scoped by tracking workspace command load success separately from the built-in fallback. Opening slash UI refreshes when needed, failures keep built-ins without suppressing later retries, and inline/palette UI now show loading, retryable error, and empty states. Added ChatViewModel regression coverage for failure then retry. Verified with export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.chat.ChatViewModelTest and ./gradlew :app:compileDebugKotlin.
