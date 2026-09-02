package dev.blazelight.p4oc.ui.screens.chat

import dev.blazelight.p4oc.domain.model.Message
import dev.blazelight.p4oc.domain.model.MessageWithParts
import dev.blazelight.p4oc.domain.model.ModelRef
import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.TokenUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MessageBlockUtilsTest {

    @Test
    fun groupMessagesIntoBlocks_preservesConsecutiveAssistantMessagesSeparately() {
        val first = assistantMessageWithText(id = "assistant-1", text = "first")
        val second = assistantMessageWithText(id = "assistant-2", text = "second")

        val blocks = groupMessagesIntoBlocks(listOf(first, second))

        assertEquals(1, blocks.size)
        val block = blocks.single() as MessageBlock.AssistantBlock
        assertEquals(listOf("assistant-1", "assistant-2"), block.messages.map { it.message.id })
        assertSame(first.message, block.messages[0].message)
        assertSame(second.message, block.messages[1].message)
    }

    @Test
    fun groupMessagesIntoBlocks_startsNewAssistantBlockAfterUserMessage() {
        val firstAssistant = assistantMessageWithText(id = "assistant-1", text = "first")
        val user = userMessageWithText(id = "user-1", text = "question")
        val secondAssistant = assistantMessageWithText(id = "assistant-2", text = "second")

        val blocks = groupMessagesIntoBlocks(listOf(firstAssistant, user, secondAssistant))

        assertEquals(3, blocks.size)
        assertEquals(listOf(firstAssistant), (blocks[0] as MessageBlock.AssistantBlock).messages)
        assertEquals(user, (blocks[1] as MessageBlock.UserBlock).message)
        assertEquals(listOf(secondAssistant), (blocks[2] as MessageBlock.AssistantBlock).messages)
    }

    @Test
    fun groupMessagesIntoBlocks_marksFollowUpBehindActiveAssistantAsQueued() {
        val activeAssistant = assistantMessageWithText(id = "assistant-1", text = "working", parentID = "user-1")
        val queuedUser = userMessageWithText(id = "user-2", text = "follow-up")

        val blocks = groupMessagesIntoBlocks(listOf(activeAssistant, queuedUser), isBusy = true)

        assertEquals(true, (blocks[1] as MessageBlock.UserBlock).isQueued)
    }

    @Test
    fun groupMessagesIntoBlocks_doesNotMarkFirstUnansweredUserMessageQueued() {
        val user = userMessageWithText(id = "user-1", text = "first")

        val blocks = groupMessagesIntoBlocks(listOf(user), isBusy = true)

        assertEquals(false, (blocks.single() as MessageBlock.UserBlock).isQueued)
    }

    @Test
    fun groupMessagesIntoBlocks_doesNotMarkQueuedWhenSessionIsIdle() {
        val activeAssistant = assistantMessageWithText(id = "assistant-1", text = "working", parentID = "user-1")
        val followUp = userMessageWithText(id = "user-2", text = "follow-up")

        val blocks = groupMessagesIntoBlocks(listOf(activeAssistant, followUp), isBusy = false)

        assertEquals(false, (blocks[1] as MessageBlock.UserBlock).isQueued)
    }

    @Test
    fun groupMessagesIntoBlocks_doesNotMarkUserQueuedWhenAssistantChildExists() {
        val activeAssistant = assistantMessageWithText(id = "assistant-1", text = "working", parentID = "user-1")
        val followUp = userMessageWithText(id = "user-2", text = "follow-up")
        val followUpAssistant = assistantMessageWithText(id = "assistant-2", text = "answer", parentID = "user-2")

        val blocks = groupMessagesIntoBlocks(listOf(activeAssistant, followUp, followUpAssistant), isBusy = true)

        assertEquals(false, (blocks[1] as MessageBlock.UserBlock).isQueued)
    }

    private fun assistantMessageWithText(
        id: String,
        text: String,
        parentID: String = "parent-1",
        completedAt: Long? = null,
    ): MessageWithParts =
        MessageWithParts(
            message = Message.Assistant(
                id = id,
                sessionID = "session-1",
                createdAt = 1L,
                completedAt = completedAt,
                parentID = parentID,
                providerID = "provider-1",
                modelID = "model-1",
                mode = "build",
                agent = "general",
                cost = 0.0,
                tokens = TokenUsage(input = 0, output = 0)
            ),
            parts = listOf(
                Part.Text(
                    id = "part-$id",
                    sessionID = "session-1",
                    messageID = id,
                    text = text
                )
            )
        )

    private fun userMessageWithText(id: String, text: String): MessageWithParts =
        MessageWithParts(
            message = Message.User(
                id = id,
                sessionID = "session-1",
                createdAt = 1L,
                agent = "general",
                model = ModelRef(providerID = "provider-1", modelID = "model-1")
            ),
            parts = listOf(
                Part.Text(
                    id = "part-$id",
                    sessionID = "session-1",
                    messageID = id,
                    text = text
                )
            )
        )
}
