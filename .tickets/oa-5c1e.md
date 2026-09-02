---
id: oa-5c1e
status: closed
deps: []
links: []
created: 2026-05-10T09:43:52Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Wire static dead-code analysis into Gradle checks

Problem:
We want to aggressively delete dead code, but the repo currently lacks an executable static-analysis gate for unused Kotlin declarations. There is a detekt.yml with UnusedPrivateMember and UnusedPrivateClass enabled, but Gradle does not apply the Detekt plugin, so no detekt task is available.

Evidence:
detekt.yml enables style.UnusedPrivateMember, style.UnusedPrivateClass, formatting.NoUnusedImports, and potential-bugs.UnreachableCode. ./gradlew tasks --all shows lint/check tasks but no detekt task. build.gradle.kts and app/build.gradle.kts do not apply a Detekt plugin.

UX Constraint:
Dead-code cleanup should be safe and boring. Static analysis findings must not encourage deleting code used by Android framework entry points, Compose previews, serialization, reflection, or resources without verification.

Expected Behavior:
A developer can run one Gradle task to report unused private Kotlin code and related unreachable/no-unused-import findings. CI/checks can include the task once the baseline is clean or explicitly baselined.

Acceptance Criteria:
- Add Detekt to the Gradle build or otherwise wire the existing detekt.yml into an executable Gradle task.
- Ensure the task analyzes app main and test Kotlin sources as appropriate.
- Keep existing ignore behavior for Preview/Composable annotations.
- Decide whether to fail immediately or introduce a baseline with documented cleanup follow-ups.
- Document the command in AGENTS.md or the project verification docs.
- Create cleanup tickets for confirmed dead-code findings rather than bulk-deleting ambiguous public/API/framework-referenced declarations.

Verification:
Run the new detekt/static-analysis task locally and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-05-10T13:33:12Z**

Implemented Detekt Gradle wiring for the app module using the existing root detekt.yml, added detekt-formatting support for NoUnusedImports, configured the task to scan app main/test/androidTest Kotlin sources, and documented :app:detekt in AGENTS.md. Initial run produced existing findings, so app/detekt-baseline.xml was generated and wired in rather than bulk-deleting ambiguous code. Created follow-up oa-0p94 for confirmed unused-code baseline cleanup. Verification passed: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:detekt; export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin.
