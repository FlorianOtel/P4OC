---
id: oa-vf6h
status: closed
deps: []
links: [oa-wmvc, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 2
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Resource todo tracker status and progress labels

Problem:
Todo/progress labels are generated as hardcoded English UI strings instead of resource-backed presentation text.

Evidence:
Domain/display audit identified TodoTracker.kt status labels and progress strings as user-facing copy outside a clear UI resource boundary.

UX Constraint:
Todo/progress text should be clear in compact chat/workspace UI and localizable. Status indicators must use the app-wide status language where possible and avoid fake precision or misleading progress.

Expected Behavior:
Todo tracker data exposes structured status/progress facts. UI formatting maps those facts to resource-backed labels and concise accessible descriptions.

Acceptance Criteria:
- Separate todo/progress state from user-facing label strings.
- Move todo status/progress labels to resources or a UI formatter using resources.
- Align status wording with the app-wide status dot semantics where applicable.
- Add tests for status/progress formatting that do not depend on domain hardcoded English.
- Preserve meaningful accessibility/content descriptions for functional indicators.

Verification:
Run targeted todo tracker/UI formatter tests and compile after implementation.


## Notes

**2026-07-07T20:42:34Z**

Implemented resource-backed todo tracker status/progress labels. TodoTracker now uses existing progress_count and percent_complete resources, central todo status constants plus todoStatusLabelRes for status labels, new todo_status_* resources, and semantic ImageVector status indicators with content descriptions on todo rows. Section icons are decorative beside visible status labels to avoid duplicate accessibility announcements; priority remains raw technical metadata. Added TodoTrackerMetadataTest for known status resource mappings and unknown fallback. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.components.todo.TodoTrackerMetadataTest; ./gradlew :app:compileDebugKotlin; ./gradlew :app:detekt (fails only on pre-existing SessionListViewModel LongMethod, ConnectionManager ReturnCount, and SessionRepositoryImplTest line-length findings; no TodoTracker findings remain).
