---
id: oa-ivm4
status: closed
deps: []
links: []
created: 2026-05-10T09:52:23Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Reuse stable Sora dynamic TextMate theme id

Problem:
SoraTextMateBootstrap loads dynamically generated TextMate themes into Sora's singleton ThemeRegistry using theme names derived from mode and theme.name.hashCode(). Repeated theme changes can accumulate registry entries for the process lifetime if the registry does not replace or remove old names.

Evidence:
SoraTextMateBootstrap.applyTheme() builds a varying themeName, creates ThemeModel(source, themeName), then calls registry.loadTheme(model) and registry.setTheme(themeName). activeThemeName is tracked but not used to remove old themes.

UX Constraint:
Switching themes should not grow memory over time. Editors should update to the selected app theme consistently.

Expected Behavior:
Use a stable dynamic theme id where loadTheme overwrites the prior model, or remove the previously active dynamic theme if the Sora API supports removal.

Acceptance Criteria:
- Replace hash-varying dynamic theme names with one stable id per necessary mode/scope, or explicitly remove old dynamic themes before loading new ones.
- Preserve dark/light correctness for TextMate color scheme.
- Verify repeated theme switches do not increase registry entries if observable.
- Keep failure fallback behavior safe.

Verification:
Run ./gradlew :app:compileDebugKotlin and manually switch themes repeatedly with a code editor open.


## Notes

**2026-05-10T15:25:41Z**

Implemented/verified stable Sora dynamic TextMate theme IDs: applyTheme now uses opencode-dynamic-dark or opencode-dynamic-light instead of including theme.name.hashCode(), so repeated theme switches reuse bounded registry names while preserving dark/light separation. Verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin passed. Manual repeated theme switch verification not run in this environment.
