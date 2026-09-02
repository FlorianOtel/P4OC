---
id: oa-7wc7
status: closed
deps: []
links: []
created: 2026-05-05T17:48:39Z
type: chore
priority: 2
assignee: Jasmin Le Roux
parent: oa-ssm2
tags: [cleanup, phase-0]
---
# Cleanup: delete 7 orphan UI components and dead modal-permission state

Delete the seven orphan Compose components flagged by the cleanup-review council that have zero callers in main: PartVisualizations.kt (drop StepStartDisplay only — keep RetryPartDisplay/StepFinishDisplay for later wiring), MultiAgentRuns.kt (full delete; AgentRunStatus has no server source), ContextUsageDisplay.kt (defer to a future ticket; delete or relocate), MessageBranching.kt (keep ForkMessageButton + simple count badge, delete the carousel/active-branch UI that invents server semantics), ErrorBoundary.kt (Compose has no error boundary semantics — actively misleading), FileAttachment.kt (full delete now; ~80 LOC of MIME helpers will be re-introduced under ui/screens/files/upload/ in Phase 7), PermissionDialogEnhanced.kt (inline prompt is canonical). Also strip dead modal-permission state in DialogQueueManager.kt: pendingPermissions queue, _pendingPermission flow, KEY_PENDING_PERMISSION* keys, persistPermissionsQueue, showNextPermission, modal branch of clearPermission. ~70 extra LOC.

## Acceptance Criteria

Files deleted. App compiles. ChatScreen still renders correctly with InlinePermissionPrompt. No grep hits for deleted symbols. Council pre-approved. Reference: file-ops-signoff.html section 6.


## Notes

**2026-05-05T18:41:38Z**

Implemented. Verified zero callers outside file for all 7 components (rg log in implementer report). Deleted: PartVisualizations.kt (338), MultiAgentRuns.kt (271), ContextUsageDisplay.kt (351), MessageBranching.kt (455), ErrorBoundary.kt (380), FileAttachment.kt (302), PermissionDialogEnhanced.kt (491). Trimmed dead modal-permission state from DialogQueueManager.kt: removed pendingPermissions queue, _pendingPermission StateFlow, KEY_PENDING_PERMISSION* keys, persistPermissionsQueue(), showNextPermission(), modal branches of clearPermission/clearPermissionByRequestId. Inline pendingPermissionsByCallId path (used by ChatMessage.kt:178-186) fully preserved. Net deletion: 2,652 LOC. Build green.
