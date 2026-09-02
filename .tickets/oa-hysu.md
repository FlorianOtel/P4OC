---
id: oa-hysu
status: closed
deps: []
links: []
created: 2026-05-10T09:42:03Z
type: chore
priority: 3
assignee: Jasmin Le Roux
---
# Centralize filename MIME type resolution

Problem:
Filename-based MIME type resolution is duplicated in chat attachment, file picker, and upload source code. This increases drift risk and makes fallback behavior inconsistent.

Evidence:
ChatViewModel.kt has mimeTypeForFilename(), FilePickerManager.kt has mimeTypeForFilename(), and ContentResolverUploadSource.kt has mimeFromName(); all use extension parsing with MimeTypeMap.getSingleton().getMimeTypeFromExtension(...).

UX Constraint:
Attachment and upload previews should classify files consistently. Unknown types should degrade predictably without surprising labels.

Expected Behavior:
Use one shared utility for resolving a MIME type from a display name/path extension, and call it from chat/file-picker/upload paths.

Acceptance Criteria:
- Add one shared helper in an appropriate core/ui-neutral location.
- Replace the three duplicated implementations with calls to the helper.
- Preserve existing fallback behavior for missing or extensionless names.
- Add a focused unit test for common extensions, uppercase extensions, and unknown/missing extension.

Verification:
Run ./gradlew :app:testDebugUnitTest for the helper tests and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-05-10T12:04:58Z**

Centralized filename MIME resolution in core/mime/FilenameMimeType. Replaced duplicate helpers in ChatViewModel, FilePickerManager, and ContentResolverUploadSource while preserving nullable fallback for picker/upload and application/octet-stream fallback for chat sends. Added focused JVM unit tests for common extensions, uppercase extensions, unknown extensions, missing names, and octet-stream fallback using an internal lookup seam because Android MimeTypeMap is not available in local unit tests. Verification: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:testDebugUnitTest --tests dev.blazelight.p4oc.core.mime.FilenameMimeTypeTest passes; export JAVA_HOME=/usr/lib/jvm/java-17-openjdk && ./gradlew :app:compileDebugKotlin passes.
