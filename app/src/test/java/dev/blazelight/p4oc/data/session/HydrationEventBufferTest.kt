package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.domain.model.Session
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.session.WorkspaceSession
import dev.blazelight.p4oc.domain.workspace.Workspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class HydrationEventBufferTest {
    private val workspace = Workspace(
        server = ServerRef.fromEndpointKey("http://test.local"),
        directory = "/repo",
    )
    private val reducer = SessionReducer(workspace)

    @Test
    fun `events during hydrate are buffered and replayed after hydrated snapshot`() {
        val buffer = HydrationEventBuffer()
        val hydrating = buffer.buffer(OpenCodeEvent.SessionCreated(session("streamed")))
        val hydratedSnapshot = Snapshot(mapOf("rest" to workspaceSession(workspace, session("rest"))))

        val liveSnapshot = buffer.replayOver(hydratedSnapshot, reducer)

        assertEquals(1, hydrating.bufferedEvents)
        assertEquals(setOf("rest", "streamed"), liveSnapshot.sessions.keys)
    }

    @Test
    fun `buffer drops oldest event when capacity is exceeded`() {
        val buffer = HydrationEventBuffer(capacity = 2)

        buffer.buffer(OpenCodeEvent.SessionCreated(session("oldest")))
        buffer.buffer(OpenCodeEvent.SessionCreated(session("middle")))
        buffer.buffer(OpenCodeEvent.SessionCreated(session("newest")))
        val liveSnapshot = buffer.replayOver(Snapshot(), reducer)

        assertEquals(setOf("middle", "newest"), liveSnapshot.sessions.keys)
        assertFalse(liveSnapshot.sessions.containsKey("oldest"))
    }

    @Test
    fun `clear removes buffered events`() {
        val buffer = HydrationEventBuffer(capacity = 2)
        buffer.buffer(OpenCodeEvent.SessionCreated(session("one")))
        buffer.buffer(OpenCodeEvent.SessionCreated(session("two")))

        buffer.clear()

        assertEquals(0, buffer.size)
        assertEquals(emptySet<String>(), buffer.replayOver(Snapshot(), reducer).sessions.keys)
    }

    @Test
    fun `replay uses snapshot while concurrent buffer can continue`() {
        val replayStarted = CountDownLatch(1)
        val finishReplay = CountDownLatch(1)
        val reducer = object : SessionReducer(workspace) {
            override fun reduce(snapshot: Snapshot, event: OpenCodeEvent): Snapshot {
                replayStarted.countDown()
                finishReplay.await()
                return super.reduce(snapshot, event)
            }
        }
        val buffer = HydrationEventBuffer(capacity = 4)
        buffer.buffer(OpenCodeEvent.SessionCreated(session("first")))
        val replayError = AtomicReference<Throwable?>()
        val replayThread = Thread {
            try {
                buffer.replayOver(Snapshot(), reducer)
            } catch (t: Throwable) {
                replayError.set(t)
            }
        }

        replayThread.start()
        replayStarted.await()
        buffer.buffer(OpenCodeEvent.SessionCreated(session("second")))
        finishReplay.countDown()
        replayThread.join()

        replayError.get()?.let { throw AssertionError("Replay failed", it) }
        assertEquals(2, buffer.size)
        assertEquals(setOf("first", "second"), buffer.replayOver(Snapshot(), this.reducer).sessions.keys)
    }

    private fun workspaceSession(workspace: Workspace, session: Session): WorkspaceSession = WorkspaceSession(
        id = SessionId(session.id),
        workspace = workspace,
        session = session,
    )

    private fun session(id: String): Session = Session(
        id = id,
        projectID = "project-$id",
        directory = "/repo",
        title = id,
        version = "1",
        createdAt = 1L,
        updatedAt = 1L,
    )
}
