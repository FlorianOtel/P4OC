package dev.blazelight.p4oc.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.blazelight.p4oc.MainActivity
import dev.blazelight.p4oc.R
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.server.ServerRef
import dev.blazelight.p4oc.domain.server.WorkspaceKey
import dev.blazelight.p4oc.ui.permission.PermissionDisplayFormatter

private const val TAG = "NotificationHelper"

internal const val MAX_NOTIFICATION_TEXT_CODE_POINTS = 512
private const val NOTIFICATION_TEXT_ELLIPSIS = "…"

internal fun boundedNotificationText(text: String?, fallback: String): String {
    val resolved = text ?: fallback
    var index = 0
    var codePoints = 0
    var lastCodePointStart = 0

    while (index < resolved.length && codePoints < MAX_NOTIFICATION_TEXT_CODE_POINTS) {
        lastCodePointStart = index
        index += Character.charCount(Character.codePointAt(resolved, index))
        codePoints++
    }

    return if (index == resolved.length) {
        resolved
    } else {
        resolved.substring(0, lastCodePointStart) + NOTIFICATION_TEXT_ELLIPSIS
    }
}

class NotificationHelper constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "user_input_required"
        const val COMPLETION_CHANNEL_ID = "assistant_completed"

        private const val PERMISSION_NOTIFICATION_ID = 0x40000000
        private const val QUESTION_NOTIFICATION_ID = 0x20000000
        private const val COMPLETION_NOTIFICATION_ID = 0x10000000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val inputChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_user_input_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_user_input_desc)
                enableVibration(true)
            }
            val completionChannel = NotificationChannel(
                COMPLETION_CHANNEL_ID,
                context.getString(R.string.notification_channel_completion_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_completion_desc)
                enableVibration(false)
                setSound(null, null)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(inputChannel)
            notificationManager.createNotificationChannel(completionChannel)
        }
    }

    fun showPermissionNotification(
        sessionId: String,
        serverRef: ServerRef,
        workspaceKey: WorkspaceKey,
        permission: Permission,
    ) {
        val route = NotificationRoute(sessionId, serverRef, workspaceKey)
        val identity = NotificationRouteCodec.identity(NotificationKind.Permission, route)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse(identity)
            NotificationRouteCodec.write(this, route)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            PERMISSION_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = boundedNotificationText(
            PermissionDisplayFormatter.title(context, permission),
            context.getString(R.string.notification_permission_required),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_permission_required))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(identity, PERMISSION_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Notification post failed (${e::class.simpleName})")
        }
    }

    fun showQuestionNotification(
        sessionId: String,
        serverRef: ServerRef,
        workspaceKey: WorkspaceKey,
        question: String?,
    ) {
        val route = NotificationRoute(sessionId, serverRef, workspaceKey)
        val identity = NotificationRouteCodec.identity(NotificationKind.Question, route)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse(identity)
            NotificationRouteCodec.write(this, route)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            QUESTION_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val questionText = boundedNotificationText(
            question,
            context.getString(R.string.notification_question_fallback),
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_question_title))
            .setContentText(questionText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(questionText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(identity, QUESTION_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Notification post failed (${e::class.simpleName})")
        }
    }

    fun showCompletionNotification(
        sessionId: String,
        serverRef: ServerRef,
        workspaceKey: WorkspaceKey,
        sessionTitle: String?,
    ) {
        val route = NotificationRoute(sessionId, serverRef, workspaceKey)
        val identity = NotificationRouteCodec.identity(NotificationKind.Completion, route)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = Uri.parse(identity)
            NotificationRouteCodec.write(this, route)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            COMPLETION_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sessionText = boundedNotificationText(
            sessionTitle,
            context.getString(R.string.notification_completion_fallback),
        )
        val notification = NotificationCompat.Builder(context, COMPLETION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_completion_title))
            .setContentText(sessionText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(identity, COMPLETION_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            AppLog.w(TAG, "Notification post failed (${e::class.simpleName})")
        }
    }

    fun clearNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
