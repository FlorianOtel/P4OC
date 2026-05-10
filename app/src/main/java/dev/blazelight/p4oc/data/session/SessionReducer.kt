package dev.blazelight.p4oc.data.session

import dev.blazelight.p4oc.data.files.ofish.OfishSessionNames
import dev.blazelight.p4oc.domain.model.OpenCodeEvent
import dev.blazelight.p4oc.domain.session.SessionId
import dev.blazelight.p4oc.domain.session.WorkspaceSession
import dev.blazelight.p4oc.domain.workspace.Workspace

open class SessionReducer(
    private val workspace: Workspace,
) {
    open fun reduce(snapshot: Snapshot, event: OpenCodeEvent): Snapshot = when (event) {
        is OpenCodeEvent.SessionCreated -> {
            if (OfishSessionNames.isOfishTitle(event.session.title)) {
                snapshot
            } else {
                snapshot.upsert(
                    event.session.let { session ->
                        WorkspaceSession(SessionId(session.id), workspace, session)
                    }
                )
            }
        }
        is OpenCodeEvent.SessionUpdated -> {
            if (OfishSessionNames.isOfishTitle(event.session.title)) {
                snapshot
            } else {
                snapshot.upsert(
                    event.session.let { session ->
                        WorkspaceSession(SessionId(session.id), workspace, session)
                    }
                )
            }
        }
        is OpenCodeEvent.SessionDeleted -> snapshot.copy(
            sessions = snapshot.sessions - event.session.id,
        )
        else -> snapshot
    }

    private fun Snapshot.upsert(session: WorkspaceSession): Snapshot = copy(
        sessions = sessions + (session.id.value to session),
    )
}
