package dev.blazelight.p4oc.core.notification

import android.content.Intent
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.domain.session.SessionId
import java.util.Base64

data class NotificationRoute(
    val sessionId: String,
    val serverRef: ServerRef,
    val workspaceKey: WorkspaceKey,
)

enum class NotificationKind(val wireName: String) {
    Permission("permission"),
    Question("question"),
    Completion("completion"),
}

object NotificationRouteCodec {
    private const val EXTRA_SESSION_ID = "notification.sessionId"
    private const val EXTRA_SERVER_ENDPOINT_KEY = "notification.serverEndpointKey"
    private const val EXTRA_WORKSPACE_TYPE = "notification.workspaceType"
    private const val EXTRA_WORKSPACE_VALUE = "notification.workspaceValue"
    private const val WORKSPACE_GLOBAL = "global"
    private const val WORKSPACE_DIRECTORY = "directory"
    private const val WORKSPACE_SESSION = "session"

    /**
     * A complete, injective PendingIntent identity. Android ignores extras when matching
     * PendingIntents, so the immutable route and notification kind also live in the data URI.
     * The URI is identity only: [read] deliberately validates the explicit extras instead.
     */
    fun identity(kind: NotificationKind, route: NotificationRoute): String {
        val (workspaceType, workspaceValue) = when (val workspace = route.workspaceKey) {
            WorkspaceKey.Global -> WORKSPACE_GLOBAL to ""
            is WorkspaceKey.Directory -> WORKSPACE_DIRECTORY to workspace.value
            is WorkspaceKey.SessionScoped -> WORKSPACE_SESSION to workspace.sessionId.value
        }
        return listOf(
            "p4oc-internal://notification/v1",
            kind.wireName,
            encodeIdentityPart(route.serverRef.endpointKey),
            workspaceType,
            encodeIdentityPart(workspaceValue),
            encodeIdentityPart(route.sessionId),
        ).joinToString("/")
    }

    private fun encodeIdentityPart(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    fun write(intent: Intent, route: NotificationRoute) {
        intent.putExtra(EXTRA_SESSION_ID, route.sessionId)
        intent.putExtra(EXTRA_SERVER_ENDPOINT_KEY, route.serverRef.endpointKey)
        when (val workspace = route.workspaceKey) {
            WorkspaceKey.Global -> intent.putExtra(EXTRA_WORKSPACE_TYPE, WORKSPACE_GLOBAL)
            is WorkspaceKey.Directory -> {
                intent.putExtra(EXTRA_WORKSPACE_TYPE, WORKSPACE_DIRECTORY)
                intent.putExtra(EXTRA_WORKSPACE_VALUE, workspace.value)
            }
            is WorkspaceKey.SessionScoped -> {
                intent.putExtra(EXTRA_WORKSPACE_TYPE, WORKSPACE_SESSION)
                intent.putExtra(EXTRA_WORKSPACE_VALUE, workspace.sessionId.value)
            }
        }
    }

    fun read(intent: Intent?): NotificationRoute? {
        intent ?: return null
        return decode(
            sessionId = intent.getStringExtra(EXTRA_SESSION_ID),
            endpointKey = intent.getStringExtra(EXTRA_SERVER_ENDPOINT_KEY),
            workspaceType = intent.getStringExtra(EXTRA_WORKSPACE_TYPE),
            workspaceValue = intent.getStringExtra(EXTRA_WORKSPACE_VALUE),
        )
    }

    /** Removes the notification payload after its route has been handled. */
    fun clear(intent: Intent?) {
        intent ?: return
        intent.removeExtra(EXTRA_SESSION_ID)
        intent.removeExtra(EXTRA_SERVER_ENDPOINT_KEY)
        intent.removeExtra(EXTRA_WORKSPACE_TYPE)
        intent.removeExtra(EXTRA_WORKSPACE_VALUE)
    }

    @Suppress("ReturnCount")
    internal fun decode(
        sessionId: String?,
        endpointKey: String?,
        workspaceType: String?,
        workspaceValue: String?,
    ): NotificationRoute? {
        val ownedSessionId = sessionId?.takeIf(String::isNotBlank) ?: return null
        val ownedEndpointKey = endpointKey?.takeIf(String::isNotBlank) ?: return null
        val workspaceKey = when (workspaceType) {
            WORKSPACE_GLOBAL -> WorkspaceKey.Global
            WORKSPACE_DIRECTORY -> workspaceValue?.takeIf(String::isNotBlank)?.let(WorkspaceKey::Directory)
            WORKSPACE_SESSION -> workspaceValue?.takeIf(String::isNotBlank)
                ?.let { WorkspaceKey.SessionScoped(SessionId(it)) }
            else -> null
        } ?: return null
        return NotificationRoute(
            sessionId = ownedSessionId,
            serverRef = ServerRef.fromEndpointKey(ownedEndpointKey),
            workspaceKey = workspaceKey,
        )
    }
}
