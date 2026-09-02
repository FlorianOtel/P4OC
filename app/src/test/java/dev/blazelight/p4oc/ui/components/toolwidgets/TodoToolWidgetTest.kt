package dev.blazelight.p4oc.ui.components.toolwidgets

import dev.blazelight.p4oc.domain.model.Part
import dev.blazelight.p4oc.domain.model.Todo
import dev.blazelight.p4oc.domain.model.ToolState
import dev.blazelight.p4oc.ui.components.todo.TODO_STATUS_CANCELLED
import dev.blazelight.p4oc.ui.components.todo.TODO_STATUS_COMPLETED
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoToolWidgetTest {
    @Test
    fun `parses todo write input without ids and applies safe defaults`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", "Check persistence")
                            put("status", "in_progress")
                            put("priority", "high")
                        }
                    )
                    add(
                        buildJsonObject {
                            put("content", "Run tests")
                            put("status", "completed")
                        }
                    )
                }
            )
        }

        val todos = todosFromToolInput(input).orEmpty()

        assertEquals(2, todos.size)
        assertEquals("tool-todo-0", todos.first().id)
        assertEquals("medium", todos.last().priority)
        assertEquals("Todos 1/2", todoCompactDescription(tool(input)))
    }

    @Test
    fun `returns null when input does not contain todo array`() {
        assertNull(todosFromToolInput(buildJsonObject { put("content", "not a list") }))
    }

    @Test
    fun `returns null when a todo content field is a nested object`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", buildJsonObject { put("text", "nested") })
                        }
                    )
                }
            )
        }

        assertNull(todosFromToolInput(input))
    }

    @Test
    fun `returns null when a todo array element is not an object`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(JsonPrimitive("plain string"))
                }
            )
        }

        assertNull(todosFromToolInput(input))
    }

    @Test
    fun `returns null when a todo content field is numeric`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", JsonPrimitive(42))
                        }
                    )
                }
            )
        }

        assertNull(todosFromToolInput(input))
    }

    @Test
    fun `returns null when a todo optional field is numeric`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", "Valid content")
                            put("status", JsonPrimitive(7))
                        }
                    )
                }
            )
        }

        assertNull(todosFromToolInput(input))
    }

    @Test
    fun `absent or null optional fields fall back to defaults`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", "No optional fields")
                        }
                    )
                    add(
                        buildJsonObject {
                            put("content", "Null optional fields")
                            put("id", JsonNull)
                            put("status", JsonNull)
                            put("priority", JsonNull)
                        }
                    )
                }
            )
        }

        val todos = todosFromToolInput(input).orEmpty()

        assertEquals(2, todos.size)
        assertEquals("tool-todo-0", todos[0].id)
        assertEquals("pending", todos[0].status)
        assertEquals("medium", todos[0].priority)
        assertEquals("tool-todo-1", todos[1].id)
        assertEquals("pending", todos[1].status)
        assertEquals("medium", todos[1].priority)
    }

    @Test
    fun `malformed elements make compact description fall back to tool name`() {
        val malformed = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", "First valid")
                        }
                    )
                    add(
                        buildJsonObject {
                            put("content", buildJsonObject { put("text", "nested") })
                        }
                    )
                }
            )
        }

        assertEquals("todowrite", todoCompactDescription(tool(malformed)))
    }

    @Test
    fun `todoread completed with empty input parses todo array output`() {
        val input = buildJsonObject { }
        val output = buildJsonArray {
            add(
                buildJsonObject {
                    put("content", "Read todos")
                    put("status", "completed")
                }
            )
            add(
                buildJsonObject {
                    put("content", "Sync state")
                    put("priority", "high")
                }
            )
        }
        val part = todoReadTool(input, output.toString())

        val todos = todosFromTool(part).orEmpty()

        assertEquals(2, todos.size)
        assertEquals("Read todos", todos.first().content)
        assertEquals(TODO_STATUS_COMPLETED, todos.first().status)
        assertEquals("high", todos.last().priority)
        assertEquals("Todos 1/2", todoCompactDescription(part))
    }

    @Test
    fun `malformed authoritative input does not fall through to valid output`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", buildJsonObject { put("text", "nested") })
                        }
                    )
                }
            )
        }
        val part = todoReadTool(input, "[{\"content\":\"Valid from output\"}]")

        assertNull(todosFromTool(part))
        assertEquals("todoread", todoCompactDescription(part))
    }

    @Test
    fun `invalid output json fails closed`() {
        val part = todoReadTool(buildJsonObject { }, "not json at all")

        assertNull(todosFromTool(part))
        assertEquals("todoread", todoCompactDescription(part))
    }

    @Test
    fun `todoread completed with metadata todos source renders the list`() {
        val metadata = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", "Metadata todo")
                            put("status", "in_progress")
                        }
                    )
                    add(
                        buildJsonObject {
                            put("content", "Metadata done")
                            put("status", "completed")
                        }
                    )
                }
            )
        }
        val part = Part.Tool(
            id = "part",
            sessionID = "session",
            messageID = "message",
            callID = "call",
            toolName = "todoread",
            state = ToolState.Completed(
                input = buildJsonObject { },
                output = "[{\"content\":\"From output\"}]",
                title = "Todo list",
                startedAt = 1,
                endedAt = 2,
                metadata = metadata,
            ),
        )

        val todos = todosFromTool(part).orEmpty()

        assertEquals(2, todos.size)
        assertEquals("Metadata todo", todos.first().content)
        assertEquals("Metadata done", todos.last().content)
        assertEquals("Todos 1/2", todoCompactDescription(part))
    }

    @Test
    fun `running metadata todos source resolves the list`() {
        val metadata = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", "In-flight todo")
                            put("status", "in_progress")
                        }
                    )
                }
            )
        }
        val part = Part.Tool(
            id = "part",
            sessionID = "session",
            messageID = "message",
            callID = "call",
            toolName = "todowrite",
            state = ToolState.Running(
                input = buildJsonObject { },
                title = "Updating todos",
                startedAt = 1,
                metadata = metadata,
            ),
        )

        val todos = todosFromTool(part).orEmpty()

        assertEquals(1, todos.size)
        assertEquals("In-flight todo", todos.first().content)
        assertEquals("Todos 0/1", todoCountLabel(todos))
    }

    @Test
    fun `metadata todos null key fails closed and does not fall through to output`() {
        val metadata = buildJsonObject {
            put("todos", JsonNull)
        }
        val part = Part.Tool(
            id = "part",
            sessionID = "session",
            messageID = "message",
            callID = "call",
            toolName = "todoread",
            state = ToolState.Completed(
                input = buildJsonObject { },
                output = "[{\"content\":\"Valid from output\"}]",
                title = "Todo list",
                startedAt = 1,
                endedAt = 2,
                metadata = metadata,
            ),
        )

        assertNull(todosFromTool(part))
        assertEquals("todoread", todoCompactDescription(part))
    }

    @Test
    fun `cancelled todos do not count toward progress`() {
        val todos = listOf(
            Todo(
                id = "t0",
                content = "Completed",
                status = TODO_STATUS_COMPLETED,
                priority = "medium",
            ),
            Todo(
                id = "t1",
                content = "Cancelled",
                status = TODO_STATUS_CANCELLED,
                priority = "medium",
            ),
            Todo(
                id = "t2",
                content = "Pending",
                status = "pending",
                priority = "medium",
            ),
        )

        assertEquals("Todos 1/3", todoCountLabel(todos))
    }

    @Test
    fun `error resolves attempted input todos but does not present them as persisted`() {
        val input = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", "Attempted but not persisted")
                            put("status", "in_progress")
                        }
                    )
                }
            )
        }
        val part = Part.Tool(
            id = "part",
            sessionID = "session",
            messageID = "message",
            callID = "call",
            toolName = "todowrite",
            state = ToolState.Error(
                input = input,
                error = "some raw protocol/stack text that must never surface",
                startedAt = 1,
                endedAt = 2,
            ),
        )

        val resolved = todosFromTool(part).orEmpty()
        assertEquals(1, resolved.size)
        assertEquals("Attempted but not persisted", resolved.first().content)

        assertNull(todoItemsForDisplay(part.state, resolved))
        // Compact presentation also hides attempted/unpersisted todo progress for a failed tool.
        assertEquals("todowrite", todoCompactDescription(part))
    }

    @Test
    fun `malformed metadata todos does not fall through to valid output`() {
        val metadata = buildJsonObject {
            put(
                "todos",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("content", JsonPrimitive(5))
                        }
                    )
                }
            )
        }
        val part = Part.Tool(
            id = "part",
            sessionID = "session",
            messageID = "message",
            callID = "call",
            toolName = "todoread",
            state = ToolState.Completed(
                input = buildJsonObject { },
                output = "[{\"content\":\"Valid from output\"}]",
                title = "Todo list",
                startedAt = 1,
                endedAt = 2,
                metadata = metadata,
            ),
        )

        assertNull(todosFromTool(part))
        assertEquals("todoread", todoCompactDescription(part))
    }

    private fun todoReadTool(input: JsonObject, output: String) = Part.Tool(
        id = "part",
        sessionID = "session",
        messageID = "message",
        callID = "call",
        toolName = "todoread",
        state = ToolState.Completed(
            input = input,
            output = output,
            title = "Todo list",
            startedAt = 1,
            endedAt = 2,
        ),
    )

    private fun tool(input: kotlinx.serialization.json.JsonObject) = Part.Tool(
        id = "part",
        sessionID = "session",
        messageID = "message",
        callID = "call",
        toolName = "todowrite",
        state = ToolState.Completed(
            input = input,
            output = "[]",
            title = "Updated todo list",
            startedAt = 1,
            endedAt = 2,
        ),
    )
}
