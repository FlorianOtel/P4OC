---
id: oa-lmh0
status: closed
deps: []
links: []
created: 2026-05-05T18:25:53Z
type: task
priority: 2
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [license, sora, compliance]
---
# LGPL-2.1 license compliance for SoraEditor

SoraEditor (https://github.com/Rosemoe/sora-editor) is LGPL-2.1, verified at https://github.com/Rosemoe/sora-editor/blob/main/LICENSE. To stay compliant when shipping in a closed-source Play Store app:

1. Add an in-app 'Open source licenses' / 'Licenses' screen — list every third-party dependency with name, version, license, and link to upstream. Include the full LGPL-2.1 text for SoraEditor specifically.
2. Display a notice in that screen that SoraEditor is dynamically integrated under LGPL-2.1 and the user has the right to replace it with a modified version. Reference upstream unmodified source at the github URL above.
3. Offer the unstripped library / relinkable object code on request — most apps do this with a contact email and 'we will provide on request' policy. Add to privacy/licenses page.
4. DO NOT modify SoraEditor's source. All integration must use public APIs (CodeEditor, ThemeRegistry, EditorColorScheme, TextMateLanguage, subscribeEvent). If we need behavior Sora doesn't expose, contribute upstream rather than fork.
5. R8/ProGuard shrinking is allowed — it's not 'modifying' under LGPL.
6. Same screen should also list other libraries (Compose, OkHttp, Retrofit, Termux libs, Coil, Koin, kotlinx, mikepenz markdown, eventsource — all permissive) for completeness.

Generate the licenses screen mostly automatically: use a Gradle plugin like 'com.mikepenz:aboutlibraries-plugin' (Apache-2.0) which scans dependencies and generates a Compose 'LibrariesContainer' screen, OR use 'com.google.android.gms:oss-licenses-plugin' (the Google one, but Play Services dependency is heavier).

Recommendation: 'com.mikepenz:aboutlibraries-plugin' — already same vendor as the markdown renderer (consistency), small, Compose-native screen out of the box.

Termux libraries already in app/build.gradle.kts:148-151 are GPL-3.0; verify they are properly attributed in the same screen — they are also LGPL-style obligations.

## Acceptance Criteria

App has a Licenses screen accessible from Settings/About. SoraEditor and Termux libraries listed with full license text, version, and upstream link. Privacy/licenses note offers object-code-on-request for LGPL deps. PR review confirms no SoraEditor source modifications (only public-API usage). Manual check: open Play Store listing — license obligations met.

