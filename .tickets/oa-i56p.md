---
id: oa-i56p
status: closed
deps: []
links: []
created: 2026-05-10T09:52:32Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Avoid fallback-theme flash on cold start

Problem:
PocketCodeTheme uses a fallback theme for the first Compose frame when ThemeLoader's in-memory cache is empty, then asynchronously loads the selected bundled theme and recomposes. On cold start this can show a visible color flash from fallback colors to the user's saved theme.

Evidence:
Theme.kt produceState initialValue is ThemeLoader.getCachedTheme(themeName, darkTheme) ?: createFallbackTheme(darkTheme), then withContext(Dispatchers.IO) loads ThemeLoader.loadBundledThemeCached(context, themeName, darkTheme). ThemeLoader cache is RAM-only.

UX Constraint:
The first visible app frame should use the user's selected theme when practical. Avoid blocking startup on expensive work, but bundled JSON theme loading is small and can be eagerly cached before UI composition if needed.

Expected Behavior:
Theme selection is available before the first drawn app frame, or the splash/loading phase hides fallback until the selected theme is ready.

Acceptance Criteria:
- Remove or mask the visible fallback-to-selected theme flash on cold start.
- Prefer eager Application/MainActivity theme cache preload or synchronous load only if measured cheap enough.
- Preserve async behavior for user-initiated theme switches if needed.
- Add a manual verification note for cold start with a non-default theme.

Verification:
Run ./gradlew :app:compileDebugKotlin and cold-launch the app with a non-default theme selected.


## Notes

**2026-05-10T11:54:17Z**

Removed the first-frame fallback theme path from PocketCodeTheme. The theme is now loaded synchronously through ThemeLoader.loadBundledThemeCached inside remember(context, themeName, darkTheme), so cold start does not compose fallback colors while the bundled theme loads asynchronously. Verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin passes. Manual cold-launch visual verification with a non-default theme is still recommended on device.
