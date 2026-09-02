package dev.blazelight.p4oc.ui.tabs

import dev.blazelight.p4oc.core.datastore.SavedServer
import dev.blazelight.p4oc.core.notification.NotificationRoute

internal fun findSavedServerForNotification(
    route: NotificationRoute,
    savedServers: List<SavedServer>,
): SavedServer? = savedServers.firstOrNull { it.endpointKey == route.serverRef.endpointKey }
