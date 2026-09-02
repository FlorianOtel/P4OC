---
id: oa-prjv
status: closed
deps: []
links: [oa-wmvc, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 2
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Resource MCP and skills status display text

Problem:
MCP and skills status display text is mapped to English strings before or outside the proper UI/resource boundary.

Evidence:
Display-boundary audit identified SkillsScreen.kt and MCP/skills status mappings as user-visible status text that may be hardcoded outside resources.

UX Constraint:
MCP/skills state must be understandable without crowding the agent workspace. Error, unavailable, loading, and ready states should use consistent app-wide status language and accessible descriptions.

Expected Behavior:
MCP/skills domain or integration layers expose structured status codes and details. UI maps those statuses to resource-backed labels, descriptions, and status indicators.

Acceptance Criteria:
- Inventory MCP/skills status strings and distinguish protocol identifiers from user-facing labels.
- Move user-facing status labels/descriptions to resources or a centralized UI formatter.
- Preserve raw server/tool identifiers only as labeled technical metadata.
- Add tests for known status mappings and unknown/error fallback text.
- Ensure status indicators have meaningful content descriptions where functional.

Verification:
Run targeted Skills/MCP formatter or screen tests and compile after implementation.


## Notes

**2026-07-07T20:35:30Z**

Implemented resource-backed MCP/skills display metadata. SkillsViewModel now exposes structured SkillInfo.status/errorDetail and SkillsErrorKind instead of English display strings. UI resolves known MCP status codes with mcpStatusDescriptionRes and R.string.skills_status_* resources, preserves server-provided errorDetail as upstream technical detail, uses resource/plural-backed tools/resources counts, and keeps raw source/tool/resource identifiers as technical metadata. Added SkillsMetadataTest coverage for known status resource mappings, unknown fallback, connected-only isEnabled behavior, and expected error kinds. Verification: ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.ui.screens.settings.SkillsMetadataTest; ./gradlew :app:compileDebugKotlin; ./gradlew :app:detekt (fails only on pre-existing SessionListViewModel LongMethod, ConnectionManager ReturnCount, and SessionRepositoryImplTest line-length findings; no oa-prjv/SkillsScreen findings remain).
