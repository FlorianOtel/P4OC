---
id: oa-cq7n
status: closed
deps: []
links: []
created: 2026-05-10T09:45:54Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Profile and reduce streaming markdown parse churn

Problem:
StreamingMarkdown receives the full concatenated markdown text for each streaming update and recreates markdown state from that content. Long assistant messages can force repeated whole-message markdown parsing/rendering during SSE token streaming.

Evidence:
StreamingMarkdown.kt calls rememberMarkdownState(content = text, retainState = true). ChatMessage passes text parts directly into StreamingMarkdown while SessionRepositoryImpl.applyDelta appends deltas to streaming text parts. The current comment says the library handles streaming with conflation, but there is no local benchmark or guard proving this is sufficient for long messages.

UX Constraint:
Long streaming responses should not jank scrolling or text input. Preserve markdown correctness, code fences, syntax highlighting, and existing visual style.

Expected Behavior:
Measure current streaming markdown cost, then either document it as acceptable or render incrementally enough that only the active tail/block reparses during streaming.

Acceptance Criteria:
- Add a benchmark, trace, or reproducible profiling note for long streaming markdown updates.
- If jank is confirmed, split rendering by stable blocks/lines/parts or use library-supported incremental/conflated APIs correctly.
- Avoid custom markdown parsing unless the library cannot support the needed behavior.
- Preserve code block/fence rendering and tertiary styling.

Verification:
Run relevant UI/performance test or manual profiling scenario plus ./gradlew :app:compileDebugKotlin.

