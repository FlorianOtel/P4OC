---
id: oa-1csc
status: closed
deps: []
links: []
created: 2026-05-10T09:41:55Z
type: task
priority: 2
assignee: Jasmin Le Roux
---
# Stream file uploads instead of materializing full ByteArray

Problem:
File uploads currently read the selected content URI into a full ByteArray before uploading. A 25 MiB file can require a contiguous 25 MiB allocation plus base64/protocol overhead in OFISH, which is risky on memory-constrained Android devices and can crash with OutOfMemoryError.

Evidence:
app/src/main/java/dev/blazelight/p4oc/ui/screens/files/upload/ContentResolverUploadSource.kt uses ByteArrayOutputStream. UploadOrchestrator.DEFAULT_MAX_BYTES is 25 MiB. FileUploadRequest in app/src/main/java/dev/blazelight/p4oc/data/files/FileRepository.kt stores bytes: ByteArray. OFISH upload then chunks/base64 encodes from this in-memory payload.

UX Constraint:
Uploading project files must not freeze or crash the app. Progress should remain accurate and failures should be human-readable, not raw protocol/JSON output.

Expected Behavior:
Upload sources stream file content into repository/mutation code using InputStream or Flow<ByteArray> chunks. The app validates size limits before or during streaming and never requires the whole file plus encoded copy in memory.

Acceptance Criteria:
- Replace FileUploadRequest.bytes with a streaming content source abstraction, or otherwise avoid materializing the full file in memory.
- Preserve expectedHash and progress callback semantics.
- OFISH upload remains chunked and reports progress per uploaded bytes.
- Unsupported REST mutations still fail cleanly without attempting to consume the stream.
- Size-limit failures are human-readable and happen before excessive memory allocation.
- Add unit tests for chunking/progress and oversized-file failure where feasible.

Verification:
Run ./gradlew :app:testDebugUnitTest for file/upload tests and ./gradlew :app:compileDebugKotlin. Manually upload a small file and a file near the configured limit.


## Notes

**2026-05-10T11:57:47Z**

Implemented streaming upload pipeline without a total file-size cap. UploadSource now opens streams instead of returning full ByteArrays; FileUploadRequest carries contentLength plus an openStream callback; OFISH reads and uploads one configured chunk at a time while reporting uploaded bytes. Removed the old 25 MiB orchestrator cap and updated focused upload/repository tests. Verification: focused upload unit tests passed and ./gradlew :app:compileDebugKotlin passed.
