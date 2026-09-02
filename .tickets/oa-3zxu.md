---
id: oa-3zxu
status: closed
deps: []
links: []
created: 2026-05-10T09:42:11Z
type: chore
priority: 3
assignee: Jasmin Le Roux
---
# Delete unused OfishPermissionAutoApprover

Problem:
OfishPermissionAutoApprover appears to be dead code. Keeping unused permission automation code around makes OFISH permission behavior harder to audit and can mislead future work into thinking background probes auto-approve permissions today.

Evidence:
app/src/main/java/dev/blazelight/p4oc/data/files/ofish/OfishPermissionAutoApprover.kt defines the class, but repository search only finds the class declaration and no instantiation or dependency wiring in FileRepositoryFactory or OFISH modules.

UX Constraint:
Permission behavior must stay explicit and human-readable. Do not introduce or imply silent permission approval for user-facing operations.

Expected Behavior:
Remove the unused class and any now-unused tests/imports. If OFISH background probes later require permission handling, add a new ticket with concrete server behavior and UX constraints.

Acceptance Criteria:
- Delete OfishPermissionAutoApprover.kt if still unused.
- Remove any stale imports/tests/docs that reference it.
- Confirm OFISH capability probe and mutation flows still compile.

Verification:
Run ./gradlew :app:compileDebugKotlin.


## Notes

**2026-05-10T11:24:23Z**

Deleted unused OfishPermissionAutoApprover.kt and its only references in OfishPermissionAutoApproverTest.kt. Verified repository search finds no remaining Kotlin references to OfishPermissionAutoApprover, PermissionAutoApprovalResult, OfishPermissionResponder, or OfishWorkspacePermissionResponder. Attempted ./gradlew :app:compileDebugKotlin, but compile is currently blocked by unrelated SettingsScreen missing string resources from oa-r8yn (settings_help/status_legend_*).
