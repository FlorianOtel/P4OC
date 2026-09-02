---
id: oa-m5a8
status: closed
deps: []
links: []
created: 2026-05-10T09:49:33Z
type: bug
priority: 1
assignee: Jasmin Le Roux
---
# Rethrow CancellationException in UI coroutine catches

Problem:
Several UI ViewModels/components catch Exception inside coroutine paths without first rethrowing CancellationException. This can swallow coroutine cancellation, write fake errors after a screen is leaving, and violate structured concurrency expectations.

Evidence:
SessionListViewModel has multiple catch (e: Exception) blocks inside viewModelScope launches. ProviderConfigViewModel and other UI classes also have blanket catches. UploadOrchestrator already handles CancellationException correctly, and safeApiCall is expected to preserve cancellation.

UX Constraint:
Navigating away, closing tabs, or cancelling jobs should not produce stale snackbar errors or keep work alive. Real failures should remain human-readable.

Expected Behavior:
Coroutine code that catches Exception/Throwable either rethrows CancellationException first or uses helper APIs that preserve cancellation.

Acceptance Criteria:
- Audit UI-layer coroutine catch blocks for catch(Exception)/catch(Throwable).
- Add catch (CancellationException) { throw it } before generic catches where the code can run in a coroutine.
- Avoid swallowing cancellation in helper functions called from coroutines.
- Add or update tests where practical for cancelled operations not setting error UI state.

Verification:
Run ./gradlew :app:testDebugUnitTest for affected ViewModels and ./gradlew :app:compileDebugKotlin.

