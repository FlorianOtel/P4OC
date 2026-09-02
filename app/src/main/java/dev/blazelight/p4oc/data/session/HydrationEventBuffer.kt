package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.domain.model.OpenCodeEvent

class HydrationEventBuffer(
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    private val lock = Any()
    private val events = ArrayDeque<OpenCodeEvent>()

    init {
        require(capacity > 0) { "Buffer capacity must be positive" }
    }

    val size: Int get() = synchronized(lock) { events.size }

    fun buffer(event: OpenCodeEvent): RepoState.Hydrating = synchronized(lock) {
        if (events.size == capacity) {
            events.removeFirst()
        }
        events.addLast(event)
        RepoState.Hydrating(bufferedEvents = events.size)
    }

    fun replayOver(snapshot: Snapshot, reducer: SessionReducer): Snapshot = drain()
        .fold(snapshot) { current, event -> reducer.reduce(current, event) }

    fun drain(): List<OpenCodeEvent> = synchronized(lock) {
        if (events.isEmpty()) return emptyList()

        val drained = events.toList()
        events.clear()
        drained
    }

    fun clear() {
        synchronized(lock) { events.clear() }
    }

    companion object {
        const val DEFAULT_CAPACITY: Int = 512
    }
}
