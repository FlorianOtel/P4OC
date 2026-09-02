---
id: oa-yzwd
status: closed
deps: []
links: []
created: 2026-05-10T09:46:03Z
type: chore
priority: 3
assignee: Jasmin Le Roux
---
# Centralize file type metadata for icons, symbols, and language scopes

Problem:
File extension classification is duplicated across editor language selection, upload visuals, file explorer icons, and file picker icons. This creates drift in how the same file type is represented in different parts of the app.

Evidence:
SoraLanguageRegistry maps filenames/extensions to TextMate scopes. UploadVisuals maps extensions to TUI glyphs. FileExplorerScreen and FilePickerDialog map extensions again to Material icons/colors. A separate ticket already covers MIME type resolution duplication.

UX Constraint:
Files should have consistent type identity across editor, file explorer, uploads, and chat attachments while preserving compact TUI presentation. Do not introduce persistent chrome or bulky labels.

Expected Behavior:
A shared file type registry/classifier maps a filename to semantic file type metadata. UI layers can derive their own icon/glyph/color/scope from that single classification instead of duplicating extension sets.

Acceptance Criteria:
- Introduce one filename-to-file-type classifier in a ui-neutral or narrowly justified UI shared package.
- Replace duplicated extension grouping in SoraLanguageRegistry, UploadVisuals, FileExplorerScreen, and FilePickerDialog where practical.
- Preserve current supported TextMate scope behavior and visual icon/color behavior.
- Add tests for representative code/config/document/image/archive/shell/build/git/lock/env/web/database files.

Verification:
Run new classifier tests and ./gradlew :app:compileDebugKotlin.


## Notes

**2026-05-10T15:22:43Z**

Implemented shared FileTypeClassifier with semantic categories and optional TextMate scopes. Refactored SoraLanguageRegistry, UploadVisuals, FileExplorerScreen, and FilePickerDialog to use it while keeping UI-specific icon/color/glyph mapping in UI layers. Added FileTypeClassifierTest covering representative code/config/document/image/archive/shell/build/git/lock/env/web/database files. Verification: targeted :app:testDebugUnitTest for classifier/Sora/upload visual tests passed; JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:compileDebugKotlin passed.
