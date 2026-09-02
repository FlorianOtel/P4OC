---
id: oa-t4t2
status: closed
deps: []
links: [oa-a6l7, oa-1n6h, oa-764s, oa-p7ei, oa-es45]
created: 2026-04-19T13:57:04Z
type: feature
priority: 2
assignee: Jasmin Le Roux
external-ref: pr-3-cherrypick
tags: [perf, benchmark, ci]
---
# Add :macrobenchmark module with startup, scroll-jank, tab-nav benchmarks

Establish performance baseline before landing PR A (connect refactor) and PR C (theme preload), so we can measure the actual wins.

Port the macrobenchmark module from PR #3 branch pr-3 (commits e8d87b5, d2318fb).

## Files to port from pr-3

- macrobenchmark/build.gradle.kts
- macrobenchmark/src/main/AndroidManifest.xml
- macrobenchmark/src/androidTest/java/dev/blazelight/p4oc/benchmark/StartupBenchmark.kt
- macrobenchmark/src/androidTest/java/dev/blazelight/p4oc/benchmark/ScrollJankBenchmark.kt
- macrobenchmark/src/androidTest/java/dev/blazelight/p4oc/benchmark/NavigateTabsBenchmark.kt
- macrobenchmark/src/androidTest/java/dev/blazelight/p4oc/benchmark/GenerateBaselineProfile.kt

## Additional work not in PR #3

- Add `benchmark` build type to app/build.gradle.kts (isDebuggable=false, isMinifyEnabled=true, signingConfig=debug, proguard files)
- Add `profileable android:shell="true"` to AndroidManifest (merged in for benchmark build only)
- Add profileinstaller dep to app module
- baselineprofile plugin + benchmark version entries in gradle/libs.versions.toml
- Register :macrobenchmark in settings.gradle.kts
- Document run command in AGENTS.md: `./gradlew :macrobenchmark:connectedBenchmarkAndroidTest` on POCO X5 5G (ADB 192.168.24.119:47293)

## Baseline measurements to capture

Run on main @ current HEAD (918c0d4) before any cherry-picks land:
- Cold startup time (no compilation / partial / baseline profile)
- Tab swipe frame timing
- Scroll jank on chat/sessions list

Save results to a baselines/ doc so PR A and PR C can compare.

## Acceptance Criteria

1. :macrobenchmark module builds (./gradlew :macrobenchmark:assemble)
2. At least StartupBenchmark runs successfully on physical device
3. Baseline numbers captured and committed to docs/ or similar
4. Doc in AGENTS.md on how to run


## Notes

**2026-04-19T16:22:25Z**

Skipped per user decision — macrobenchmark setup has issues (profileable/debuggable build type, device quirks) that aren't worth solving right now. Wins from PR A (oa-p7ei) and PR C (oa-1n6h) will be validated by manual timing / StrictMode / logs instead of benchmark numbers.
