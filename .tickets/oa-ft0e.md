---
id: oa-ft0e
status: closed
deps: []
links: []
created: 2026-05-09T15:57:54Z
type: feature
priority: 2
assignee: Jasmin Le Roux
---
# Move branch metadata into chat overflow

Problem:\nThe chat header has limited space on phones and currently branch metadata competes with higher-value controls/status. The app should maximize agent transcript space and avoid persistent secondary metadata in cramped headers.\n\nEvidence:\nChat header currently renders connection dot, branch text, todo count, and overflow actions. Branch is useful but secondary compared with running/connection state and core navigation/actions.\n\nUX Constraint:\nUI chrome must be heavily justified. Branch metadata should be accessible but should not consume persistent header width when space is constrained.\n\nExpected Behavior:\nMove branch metadata from persistent chat header text into the chat overflow menu or an equally compact non-persistent surface. The overflow entry should show the current branch and optionally provide copy/open git-related actions if existing patterns support it.\n\nAcceptance Criteria:\n- Chat header no longer shows persistent branch text on compact phone layouts.\n- Chat overflow shows the current branch when available.\n- Branch display remains human-readable and truncated safely for long branch names.\n- Connection/running indicators and primary actions remain visible.\n- No workspace/project identity is hidden by this change; workspace concerns remain separately handled.\n\nVerification:\n- Verify chat header on narrow phone width has more room for core agent controls.\n- Verify overflow displays branch when branch data exists and omits it cleanly when absent.


## Notes

**2026-05-10T12:06:00Z**

Decision: leave branch metadata in the current chat header for now and close this ticket without code changes. Moving it into overflow would either create non-actionable menu content, which feels broken, or require turning branch display into a copy action that adds behavior/chrome not clearly justified. Current compact header remains acceptable unless narrow-width testing shows branch text is actively crowding core controls.
