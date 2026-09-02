---
id: oa-1n6h
status: closed
deps: []
links: [oa-a6l7, oa-t4t2, oa-764s, oa-p7ei, oa-es45]
created: 2026-04-19T13:58:33Z
type: feature
priority: 2
assignee: Jasmin Le Roux
external-ref: pr-3-cherrypick
tags: [ui, perf, theme, startup]
---
# Async theme preload — no more blank first frame

Port OptimizedThemeLoader pattern from PR #3 branch pr-3 (files app/src/main/java/dev/blazelight/p4oc/ui/theme/opencode/OptimizedThemeLoader.kt and ThemeCacheManager.kt). First frame uses hardcoded fallback ColorScheme from createFallbackTheme(isDark); real theme JSON loads async on Dispatchers.IO; Compose recomposes with real theme when ready.

## What to port

From pr-3:
- app/src/main/java/dev/blazelight/p4oc/ui/theme/opencode/OptimizedThemeLoader.kt (full file)
- app/src/main/java/dev/blazelight/p4oc/ui/theme/opencode/ThemeCacheManager.kt (full file)
- Theme.kt wiring so loadThemeImmediate() is the primary Compose-time read

## Fix PR #3's runBlocking

SettingsDataStore.init in pr-3 uses runBlocking to synchronously load cached values. DO NOT PORT THAT.

Instead:
- Keep `@Volatile var cachedThemeName: String = DEFAULT_THEME_NAME` (starts with default)
- Fire async scope.launch in init that reads DataStore and updates cached values
- First composition reads DEFAULT_THEME_NAME; when async read completes, LiveData/Flow triggers recomposition with the saved theme
- Apply same treatment to cachedServerUrl and cachedUsername (already Flow-based on main, just confirm no regression)

## Do NOT

- runBlocking anywhere in init paths
- Change DEFAULT_THEME_NAME from catppuccin to dracula (rejected)
- Add DATASTORE_DEBUG Log.d (rejected)
- Port anything UI-side from pr-3 (kotlin theming only)

## Blocks on

- oa-t4t2 (PR E) to measure first-frame improvement

## Verify

- Cold start on POCO: measure time from icon tap to first content paint
- Before: blank/default first frame for ~200-400ms
- After: default theme first frame instant, real theme in < 100ms
- No change in any user-visible theme behavior after initial load

## Acceptance Criteria

1. Compiles cleanly
2. Cold-start first frame shows default catppuccin fallback within 50ms
3. Real theme replaces within 200ms
4. No runBlocking in SettingsDataStore
5. Default theme stays catppuccin on fresh install
6. Benchmark shows improvement in StartupBenchmark (from PR E)

