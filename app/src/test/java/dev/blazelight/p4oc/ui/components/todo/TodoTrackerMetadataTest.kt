package dev.blazelight.p4oc.ui.components.todo

import dev.blazelight.p4oc.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoTrackerMetadataTest {
    @Test
    fun `known todo statuses map to localized label resources`() {
        val expected = mapOf(
            TODO_STATUS_PENDING to R.string.todo_status_pending,
            TODO_STATUS_IN_PROGRESS to R.string.todo_status_in_progress,
            TODO_STATUS_COMPLETED to R.string.todo_status_completed,
            TODO_STATUS_CANCELLED to R.string.todo_status_cancelled,
        )

        expected.forEach { (status, labelRes) ->
            assertEquals(labelRes, todoStatusLabelRes(status))
        }
    }

    @Test
    fun `unknown todo status maps to unknown label resource`() {
        assertEquals(R.string.todo_status_unknown, todoStatusLabelRes("blocked"))
    }
}
