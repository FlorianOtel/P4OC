---
id: oa-ivwp
status: closed
deps: []
links: [oa-wmvc, oa-erzs, oa-12ui]
created: 2026-07-05T18:06:47Z
type: bug
priority: 1
assignee: Jasmin Le Roux
parent: oa-nwha
---
# Resource notification channel and event text

Problem:
Notification user-facing strings are hardcoded or formatted outside a resource-backed notification boundary. This repeats the wrong-layer display problem from permission titles on a separate surface with its own Android context and notification-channel constraints.

Evidence:
Audit identified NotificationHelper.kt notification channel/title/body strings and NotificationEventObserver permission notification text as user-visible display copy. Permission-specific title formatting is tracked in oa-wmvc, but notification channel names, notification titles, and non-permission event bodies need their own resource-backed behavior.

UX Constraint:
Background notifications must remain concise, human-readable, and localized where possible. They must not surface raw protocol/JSON payloads or internal identifiers as primary text unless intentionally labeled as technical detail.

Expected Behavior:
Notification channels, titles, and bodies are produced at the notification/UI boundary using Android resources and typed event data. Unknown event types still produce a safe, human-readable fallback.

Acceptance Criteria:
- Inventory notification channel names, notification titles, and body strings emitted by NotificationHelper/NotificationEventObserver.
- Move user-facing strings and format templates to resources.
- Keep permission title formatting aligned with oa-wmvc without duplicating domain display text.
- Add/adjust tests for notification text generation using a Context/resource-backed formatter where practical.
- Unknown protocol events produce a resource-backed fallback and do not expose raw JSON as user-facing body text.

Verification:
Run targeted notification formatter/observer tests and compile with JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin.


## Notes

**2026-07-07T20:12:41Z**

Moved notification channel names/descriptions and event notification titles/fallback bodies to Android string resources. NotificationHelper now uses context.getString for user-input and completion channel metadata, question title, question fallback body, completion title, and completion fallback body. NotificationEventObserver no longer owns the hardcoded 'AI has a question' fallback; it passes nullable question text to the notification boundary. Permission notification title remains aligned with PermissionDisplayFormatter and R.string.notification_permission_required. Verification: grep found no remaining hardcoded notification user-facing strings in core/notification; ./gradlew :app:compileDebugKotlin passed. Unit tests were not added because this project has no Robolectric/resource-backed JVM test setup, so resource correctness is compile-verified.
