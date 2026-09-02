package dev.blazelight.p4oc.ui.components.question

import dev.blazelight.p4oc.domain.model.Question
import dev.blazelight.p4oc.domain.model.QuestionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineQuestionCardTest {

    @Test
    fun `custom answer option is hidden when question disallows custom answers`() {
        val question = Question(
            header = "Pick",
            question = "Choose one",
            options = listOf(QuestionOption("A", "Option A")),
            custom = false
        )

        assertFalse(shouldShowCustomAnswerOption(question))
    }

    @Test
    fun `custom answer option is shown when question allows custom answers`() {
        val question = Question(
            header = "Pick",
            question = "Choose one",
            options = listOf(QuestionOption("A", "Option A")),
            custom = true
        )

        assertTrue(shouldShowCustomAnswerOption(question))
    }

    @Test
    fun `sanitizeAnswersForQuestion strips stale custom answers when custom is false`() {
        val question = Question(
            header = "Pick",
            question = "Choose one",
            options = listOf(QuestionOption("A", "Option A"), QuestionOption("B", "Option B")),
            custom = false
        )

        val sanitized = sanitizeAnswersForQuestion(question, listOf("A", "custom text"))

        assertEquals(listOf("A"), sanitized)
    }

    @Test
    fun `sanitizeAnswersForQuestion preserves custom answers when custom is true`() {
        val question = Question(
            header = "Pick",
            question = "Choose one",
            options = listOf(QuestionOption("A", "Option A")),
            custom = true
        )

        val sanitized = sanitizeAnswersForQuestion(question, listOf("A", "custom text"))

        assertEquals(listOf("A", "custom text"), sanitized)
    }
}
