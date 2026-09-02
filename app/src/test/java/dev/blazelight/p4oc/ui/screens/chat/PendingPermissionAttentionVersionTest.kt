@file:Suppress("ImportOrdering")

package dev.blazelight.p4oc.ui.screens.chat

import dev.blazelight.p4oc.domain.model.Message
import dev.blazelight.p4oc.domain.model.MessageWithParts
import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.ToolState
import dev.blazelight.p4oc.domain.model.TokenUsage
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPermissionAttentionVersionTest {

    @Test
    fun `permission arrival changes the attention version`() {
        val before = pendingPermissionAttentionVersion(emptySet())
        val after = pendingPermissionAttentionVersion(setOf("call_abc"))

        assertNotEquals(
            "Pending permission arrival must change the tail-attention version so the chat scrolls to show it",
            before,
            after
        )
    }

    @Test
    fun `permission resolution changes the attention version`() {
        val pending = pendingPermissionAttentionVersion(setOf("call_abc"))
        val resolved = pendingPermissionAttentionVersion(emptySet())

        assertNotEquals("Resolving a permission must change the version", pending, resolved)
    }

    @Test
    fun `same permissions produce same version`() {
        val a = pendingPermissionAttentionVersion(setOf("call_abc", "call_def"))
        val b = pendingPermissionAttentionVersion(setOf("call_def", "call_abc"))

        assertEquals("Same permission set must produce same version regardless of order", a, b)
    }

    @Test
    fun `new permission requests attention even when older permission remains pending`() {
        assertTrue(hasNewPendingPermission(setOf("call_abc"), setOf("call_abc", "call_def")))
    }

    @Test
    fun `resolution does not request another reveal`() {
        assertFalse(hasNewPendingPermission(setOf("call_abc", "call_def"), setOf("call_def")))
    }

    @Test
    fun `permission reveal targets its existing message block`() {
        val blocks = groupMessagesIntoBlocks(
            listOf(
                assistantMessage("message-1", "call_old"),
                assistantMessage("message-2", "call_pending"),
            )
        )

        assertEquals(0, pendingPermissionBlockIndex(blocks, setOf("call_pending")))
    }

    @Test
    fun `callID-less permission is rendered as session pending`() {
        val permission = permission(id = "permission-1", callId = null)

        assertEquals(
            listOf(permission),
            unmatchedPendingPermissions(
                messages = listOf(assistantMessage("message-1", "call-1")),
                pendingPermissionsByKey = mapOf("permission:permission-1" to permission),
            ),
        )
    }

    @Test
    fun `tool-bound permission is not duplicated as session pending`() {
        val permission = permission(id = "permission-1", callId = "call-1")

        assertTrue(
            unmatchedPendingPermissions(
                messages = listOf(assistantMessage("message-1", "call-1")),
                pendingPermissionsByKey = mapOf("call-1" to permission),
            ).isEmpty(),
        )
    }

    @Test
    fun `permission with missing tool is rendered as session pending`() {
        val permission = permission(id = "permission-1", callId = "call-missing")

        assertEquals(
            listOf(permission),
            unmatchedPendingPermissions(
                messages = listOf(assistantMessage("message-1", "call-other")),
                pendingPermissionsByKey = mapOf("call-missing" to permission),
            ),
        )
    }

    @Test
    fun `empty history with pending question is content`() {
        assertTrue(
            hasChatContent(
                hasMessages = false,
                isBusy = false,
                hasPendingQuestion = true,
                hasSessionPendingPermissions = false,
            ),
        )
    }

    @Test
    fun `empty history with unmatched permission is content`() {
        val sessionPermissions = unmatchedPendingPermissions(
            messages = emptyList(),
            pendingPermissionsByKey = mapOf(
                "permission:permission-1" to permission(id = "permission-1", callId = null),
            ),
        )

        assertTrue(
            hasChatContent(
                hasMessages = false,
                isBusy = false,
                hasPendingQuestion = false,
                hasSessionPendingPermissions = sessionPermissions.isNotEmpty(),
            ),
        )
    }

    @Test
    fun `truly empty idle session has no content`() {
        assertFalse(
            hasChatContent(
                hasMessages = false,
                isBusy = false,
                hasPendingQuestion = false,
                hasSessionPendingPermissions = false,
            ),
        )
    }

    private fun permission(id: String, callId: String?) = Permission(
        id = id,
        type = "bash",
        patterns = listOf("pwd"),
        sessionID = "session-1",
        messageID = "",
        callID = callId,
        metadata = buildJsonObject {},
        always = emptyList(),
    )

    private fun assistantMessage(messageId: String, callId: String) = MessageWithParts(
        message = Message.Assistant(
            id = messageId,
            sessionID = "session-1",
            parentID = "parent-1",
            createdAt = 1L,
            modelID = "model-1",
            providerID = "provider-1",
            mode = "agent",
            agent = "build",
            path = null,
            cost = 0.0,
            tokens = TokenUsage(input = 0, output = 0),
            completedAt = null,
            error = null,
            summary = null,
        ),
        parts = listOf(
            Part.Tool(
                id = "part-$callId",
                sessionID = "session-1",
                messageID = messageId,
                callID = callId,
                toolName = "bash",
                state = ToolState.Pending(buildJsonObject {}, ""),
            )
        ),
    )
}
