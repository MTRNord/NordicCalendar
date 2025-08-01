package space.midnightthoughts.nordiccalendar.notifications

import android.app.Notification.CATEGORY_EVENT
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import space.midnightthoughts.nordiccalendar.R

/**
 * NotificationHelper is a utility object for managing and displaying event notifications.
 * It handles notification channel creation, permission checks, and building notifications
 * for calendar events with reminders.
 */
object NotificationHelper {
    /**
     * Notification channel ID for calendar event reminders.
     */
    private const val CHANNEL_ID = "calendar_event_reminders"

    /**
     * Shows a notification for a calendar event reminder.
     *
     * @param context The application context.
     * @param eventId The ID of the event.
     * @param eventTitle The title of the event.
     * @param eventDescription The description of the event (optional).
     * @param eventTime The start time of the event in milliseconds.
     * @param eventEndTime The end time of the event in milliseconds.
     * @param eventLocation The location of the event.
     */
    fun showEventNotification(
        context: Context,
        eventId: Long,
        eventTitle: String,
        eventDescription: String? = "",
        eventTime: Long,
        eventEndTime: Long,
        eventLocation: String
    ) {
        Log.d(
            "NotificationHelper",
            "showEventNotification: eventId=$eventId, eventTitle=$eventTitle, eventTime=$eventTime, eventDescription=$eventDescription, eventEndTime=$eventEndTime, eventLocation=$eventLocation"
        )
        createNotificationChannel(context)
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(
                    "NotificationHelper",
                    "POST_NOTIFICATIONS permission not granted, notification not shown"
                )
                // No permission, do not show notification
                return
            }
        }
        // Deep link intent for EventDetails
        val deepLinkUri = "nordiccalendar://eventdetails/$eventId".toUri()
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val startCal = java.util.Calendar.getInstance().apply { timeInMillis = eventTime }
        val endCal = java.util.Calendar.getInstance().apply { timeInMillis = eventEndTime }
        val sameDay =
            startCal.get(java.util.Calendar.YEAR) == endCal.get(java.util.Calendar.YEAR) &&
                    startCal.get(java.util.Calendar.DAY_OF_YEAR) == endCal.get(java.util.Calendar.DAY_OF_YEAR)
        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
        val dateFormat = android.text.format.DateFormat.getDateFormat(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_calendar_clock_24)
            .setContentTitle(eventTitle.ifEmpty { context.getString(R.string.notification_default_title) })
            .setContentText(
                if (sameDay) {
                    "${timeFormat.format(startCal.time)} — ${timeFormat.format(endCal.time)} $eventDescription"
                } else {
                    "${dateFormat.format(startCal.time)} ${timeFormat.format(startCal.time)} — ${
                        dateFormat.format(endCal.time)
                    } ${timeFormat.format(endCal.time)} $eventDescription"
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (eventLocation.isNotBlank()) {
            try {
                val mapsIntent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=${eventLocation}".toUri())
                val mapsPendingIntent = PendingIntent.getActivity(
                    context,
                    (eventId + 1000).toInt(),
                    mapsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    0,
                    context.getString(R.string.notification_action_open),
                    pendingIntent
                )
                builder.addAction(
                    0,
                    context.getString(R.string.notification_action_show_route),
                    mapsPendingIntent
                )
            } catch (e: Exception) {
                builder.addAction(
                    0,
                    context.getString(R.string.notification_action_open),
                    pendingIntent
                )
            }
        } else {
            builder.addAction(
                0,
                context.getString(R.string.notification_action_open),
                pendingIntent
            )
        }

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(eventId.toInt(), builder.build())
                Log.d("NotificationHelper", "Notification sent for eventId=$eventId")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "SecurityException: ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        Log.d("NotificationHelper", "Creating notification channel: $CHANNEL_ID")
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
