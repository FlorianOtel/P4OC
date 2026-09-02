---
id: oa-wmvc
status: closed
deps: []
links: [oa-nwha, oa-ivwp, oa-vf6h, oa-prjv, oa-3l1w, oa-0mel, oa-12ui, oa-casy, oa-3yk2, oa-qy0f, oa-wxf2, oa-dygk]
created: 2026-05-10T09:49:51Z
type: task
priority: 3
assignee: Jasmin Le Roux
---
# file. Permission domain model stores title: String. InlinePermissionPrompt renders permission.title directly, and notifications also use event.permission.title.

UX Constraint:
Permission prompts and background notifications must remain clear and human-readable, but display text should be produced at UI/notification boundaries using resources when possible.

Expected Behavior:
EventMapper preserves raw permission type/patterns/metadata. UI/notification layers map permission type to localized string resources or a typed PermissionKind.

Acceptance Criteria:
- Replace data-layer English title generation with raw/typed permission data.
- Add PermissionKind enum/sealed mapping if useful, preserving unknown permission fallback.
- InlinePermissionPrompt uses stringResource for known permission action text.
- NotificationEventObserver gets localized/human-readable permission titles from Android context/resources.
- Preserve pattern display and unknown permission behavior.

Verification:
Run mapper/UI tests where available and ./gradlew :app:compileDebugKotlin.

Problem:
EventMapper converts raw permission tokens into English display titles in the data layer. This bakes UI language into domain data and bypasses string resources/localization.

Evidence:
Mappers.kt generatePermissionTitle maps bash/shell/edit/write/webfetch/etc to English strings such as Execute


## Notes

**2026-07-05T17:00:17Z**

Red-test/audit context from 2026-07-05:

This ticket is the permission-title complaint in the four-complaint red-test batch tracked by parent oa-casy.

Additional evidence:
- Permission.kt:20-35 exposes Permission.title and computes English labels such as Execute command, Write to file, Run sub-agent, then appends protocol pattern data.
- InlinePermissionPrompt.kt:50-52 renders permission.title directly.
- NotificationEventObserver.kt:95-101 passes the same preformatted permission title into notification code.
- EventMapperTest.kt:120-154 currently asserts an English mapper/domain title, e.g. Execute command: rm -rf /tmp/test, which preserves the wrong layer boundary.
- ToolStateExtTest.kt:102-121 contains the desired red contract that Permission domain should not expose localized display title, but it should be strengthened after the production fix so it is not only reflection-based.

Red-test expectation:
- Data/domain tests should assert raw permission fields are preserved: id, type/kind, patterns, sessionID, messageID, callID, always, and metadata.
- UI/notification formatter tests should assert known permission kinds map through resources and preserve pattern display separately.
- Tests should not assert English display copy from EventMapper/domain models.

Implementation direction:
Keep permission type/kind/patterns as domain data. Move kind-to-human text and title formatting to UI/notification boundary using string resources and Context.getString/stringResource. Preserve unknown permission fallback with resource-backed unknown format plus raw protocol code as data.

**2026-07-06T11:29:49Z**

Completion update from 2026-07-06:

Completed the permission localization boundary fix.

Production changes:
- Removed Permission.title from the domain model; Permission now preserves raw type/patterns/metadata/always plus typed kind mapping only.
- Added PermissionDisplayFormatter at the UI/Android boundary for known PermissionKind resource mapping, unknown permission fallback, and pattern-preserving title formatting.
- InlinePermissionPrompt now renders permission display text via stringResource-backed formatter logic instead of domain Permission.title.
- NotificationHelper now formats permission notification content with Android Context/resources and uses the existing notification_permission_required title resource.
- NotificationEventObserver passes the raw Permission to NotificationHelper rather than preformatted title text.
- Added permission action/title string resources for known kinds, unknown fallback, and pattern formatting.

Tests:
- Strengthened ToolStateExtTest to assert Permission preserves raw transport fields and does not expose title/displayTitle String APIs.
- Updated EventMapperTest away from stale English title assertions for permission v1/v2 events and toward raw field/kind/metadata preservation.
- Added PermissionDisplayFormatterTest for known kind resource ids, unknown fallback capitalization, and title composition preserving pattern display.

Verification:
- ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.domain.model.ToolStateExtTest --tests dev.blazelight.p4oc.data.remote.mapper.EventMapperTest --tests dev.blazelight.p4oc.ui.permission.PermissionDisplayFormatterTest -> PASS
- ./gradlew :app:compileDebugKotlin -> PASS
- ./gradlew :app:detekt -> PASS
- ./gradlew :app:testDebugUnitTest -> PASS
