---
id: oa-tqsm
status: closed
deps: []
links: []
created: 2026-05-10T09:46:14Z
type: chore
priority: 3
assignee: Jasmin Le Roux
---
# Delete assistant message block grouping if server parts already preserve structure

Problem:
MessageBlockUtils groups consecutive assistant MessageWithParts entries into one AssistantBlock by flattening all parts onto the first assistant message. If OpenCode consistently models a single assistant turn as one MessageWithParts with ordered parts, this grouping is extra state-shaping and can hide message-level metadata or errors from later assistant messages.

Evidence:
MessageBlockUtils.groupMessagesIntoBlocks() collects consecutive Message.Assistant objects and MessageBlockView flattens block.messages.flatMap { it.parts } into a MessageWithParts using block.messages.first().message. The server/domain model already has MessageWithParts for text/tool/reasoning parts inside a message.

UX Constraint:
Chat should render the server's message/part structure faithfully. Do not lose assistant message errors, metadata, branching/revert identity, or ordering. Preserve current compact visual grouping only if there is a real server behavior that requires it.

Expected Behavior:
Either delete assistant-message grouping and render each MessageWithParts directly, or document/test the concrete server scenario requiring grouping.

Acceptance Criteria:
- Verify whether consecutive assistant messages occur in real API/SSE history and why.
- If not needed, remove MessageBlock.AssistantBlock flattening and simplify ChatScreen rendering.
- If needed, preserve all message-level metadata/errors when grouping.
- Add tests for consecutive assistant messages with distinct errors/metadata to prevent silent loss.

Verification:
Run chat rendering/unit tests and ./gradlew :app:compileDebugKotlin.

